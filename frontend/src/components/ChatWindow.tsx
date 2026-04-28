import { FormEvent, useEffect, useRef, useState } from "react";
import { uploadDocument } from "../services/document";
import { streamChat } from "../services/chat";
import { SourceReferenceView, StreamEvent } from "../types/chat";

const THREAD_KEY = "spring-rag-thread-id";

function createThreadId(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `thread-${Date.now()}`;
}

function getOrCreateThreadId(): string {
  const existing = window.localStorage.getItem(THREAD_KEY);
  if (existing) {
    return existing;
  }

  const nextThreadId = createThreadId();
  window.localStorage.setItem(THREAD_KEY, nextThreadId);
  return nextThreadId;
}

type MessageItem = {
  role: "user" | "assistant";
  content: string;
};

export function ChatWindow() {
  const [threadId, setThreadId] = useState("");
  const [input, setInput] = useState("");
  const [messages, setMessages] = useState<MessageItem[]>([]);
  const [sources, setSources] = useState<SourceReferenceView[]>([]);
  const [error, setError] = useState("");
  const [handoffNotice, setHandoffNotice] = useState("");
  const [isStreaming, setIsStreaming] = useState(false);

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadSuccess, setUploadSuccess] = useState("");
  const [uploadError, setUploadError] = useState("");
  const [uploadedFiles, setUploadedFiles] = useState<string[]>([]);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    setThreadId(getOrCreateThreadId());
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmed = input.trim();
    if (!trimmed || !threadId || isStreaming) {
      return;
    }

    const userMessage: MessageItem = { role: "user", content: trimmed };
    setMessages((previous) => [...previous, userMessage, { role: "assistant", content: "" }]);
    setInput("");
    setError("");
    setHandoffNotice("");
    setIsStreaming(true);

    try {
      for await (const streamEvent of streamChat({ threadId, message: trimmed })) {
        applyStreamEvent(streamEvent);
      }
    } catch (streamError) {
      setError(streamError instanceof Error ? streamError.message : "消息流处理失败，请稍后重试。");
    } finally {
      setIsStreaming(false);
    }
  }

  async function handleUpload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedFile || isUploading) {
      return;
    }

    setIsUploading(true);
    setUploadError("");
    setUploadSuccess("");

    try {
      const uploadedDocument = await uploadDocument(selectedFile);
      setUploadSuccess(`Uploaded and indexed: ${uploadedDocument.fileName}`);
      setUploadedFiles((previous) => [...previous, uploadedDocument.fileName]);
      setSelectedFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    } catch {
      setUploadError("Document upload failed. Please retry after checking the backend service.");
    } finally {
      setIsUploading(false);
    }
  }

  function applyStreamEvent(streamEvent: StreamEvent) {
    if (streamEvent.type === "chunk") {
      setMessages((previous) => {
        const next = [...previous];
        const last = next[next.length - 1];
        if (last && last.role === "assistant") {
          next[next.length - 1] = {
            ...last,
            content: last.content + streamEvent.content
          };
        }
        return next;
      });
      return;
    }

    if (streamEvent.type === "sources") {
      setSources(streamEvent.sources);
      return;
    }

    if (streamEvent.type === "handoff") {
      setHandoffNotice(streamEvent.message);
    }
  }

  return (
    <main className="page-shell">
      <section className="hero-card">
        <p className="eyebrow">企业级 AI 客服骨架</p>
        <h1>Spring RAG 客服控制台</h1>
        <p className="subtitle">
          线程 ID{" "}
          <span className="thread-chip">{threadId || "生成中..."}</span>
        </p>
      </section>

      <section className="workspace-grid">
        <section className="panel">
          <header className="panel-header">
            <h2>对话窗口</h2>
            <span>{isStreaming ? "流式返回中" : "等待输入"}</span>
          </header>

          <section className="upload-card">
            <div className="upload-copy">
              <h3>上传知识文档</h3>
              <p>上传后会立即写入知识库，你可以继续在当前页面提问。</p>
            </div>

            <form className="upload-form" onSubmit={handleUpload}>
              <label className="upload-label" htmlFor="document-upload">
                Select document
              </label>
              <input
                ref={fileInputRef}
                id="document-upload"
                type="file"
                onChange={(event) => setSelectedFile(event.target.files?.[0] ?? null)}
              />
              <button type="submit" disabled={!selectedFile || isUploading}>
                {isUploading ? "Uploading..." : "Upload document"}
              </button>
            </form>

            {uploadSuccess ? <p className="status success">{uploadSuccess}</p> : null}
            {uploadError ? <p className="status error">{uploadError}</p> : null}

            <div className="upload-history">
              <strong>Uploaded in this session</strong>
              {uploadedFiles.length === 0 ? (
                <p className="upload-empty">No documents uploaded yet.</p>
              ) : (
                <ul className="upload-list">
                  {uploadedFiles.map((fileName) => (
                    <li key={fileName}>{fileName}</li>
                  ))}
                </ul>
              )}
            </div>
          </section>

          <div className="message-list">
            {messages.length === 0 ? (
              <div className="empty-state">输入一条消息，体验后端 SSE 流式回复。</div>
            ) : (
              messages.map((message, index) => (
                <article key={`${message.role}-${index}`} className={`message-bubble ${message.role}`}>
                  <strong>{message.role === "user" ? "用户" : "助手"}</strong>
                  <p>{message.content || "..."}</p>
                </article>
              ))
            )}
          </div>

          <form className="composer" onSubmit={handleSubmit}>
            <textarea
              value={input}
              onChange={(event) => setInput(event.target.value)}
              placeholder="请输入你的问题，例如：设备保修多久？"
              rows={4}
            />
            <button type="submit" disabled={isStreaming || !threadId}>
              {isStreaming ? "发送中..." : "发送消息"}
            </button>
          </form>

          {error ? <p className="status error">{error}</p> : null}
          {handoffNotice ? <p className="status warn">{handoffNotice}</p> : null}
        </section>

        <aside className="panel side-panel">
          <header className="panel-header">
            <h2>参考来源</h2>
            <span>一期占位</span>
          </header>

          {sources.length === 0 ? (
            <div className="empty-state">消息返回后，这里会展示知识片段来源。</div>
          ) : (
            <div className="source-list">
              {sources.map((source, index) => (
                <article key={`${source.title}-${index}`} className="source-card">
                  <strong>{source.title}</strong>
                  <p>{source.snippet}</p>
                </article>
              ))}
            </div>
          )}
        </aside>
      </section>
    </main>
  );
}
