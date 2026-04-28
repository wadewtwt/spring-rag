import { ChatRequest, SourceReferenceView, StreamEvent } from "../types/chat";

function parseDataLines(rawPayload: string): string {
  return rawPayload
    .split("\n")
    .filter((line) => line.startsWith("data:"))
    .map((line) => line.slice(5).trim())
    .join("\n");
}

function mapEvent(eventName: string, payload: string): StreamEvent | null {
  if (eventName === "chunk") {
    return { type: "chunk", content: payload };
  }

  if (eventName === "sources") {
    try {
      return { type: "sources", sources: JSON.parse(payload) as SourceReferenceView[] };
    } catch {
      return { type: "sources", sources: [] };
    }
  }

  if (eventName === "handoff") {
    return { type: "handoff", message: payload };
  }

  if (eventName === "complete") {
    return { type: "complete" };
  }

  return null;
}

export async function* streamChat(request: ChatRequest): AsyncGenerator<StreamEvent> {
  const response = await fetch(`/api/chat/stream`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(request)
  });

  if (!response.ok || !response.body) {
    throw new Error("后端聊天服务暂不可用，请确认 8089 端口服务已启动。");
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder("utf-8");
  let buffer = "";

  while (true) {
    const { value, done } = await reader.read();
    if (done) {
      break;
    }

    buffer += decoder.decode(value, { stream: true });
    const events = buffer.split("\n\n");
    buffer = events.pop() ?? "";

    for (const rawEvent of events) {
      const lines = rawEvent.split("\n");
      const eventLine = lines.find((line) => line.startsWith("event:")) ?? "";
      const eventName = eventLine.replace("event:", "").trim();
      const payload = parseDataLines(rawEvent);
      // 这里把原始 SSE 文本转换成前端可消费的事件对象，后续替换协议时只改这一层。
      const mapped = mapEvent(eventName, payload);
      if (mapped) {
        yield mapped;
      }
    }
  }
}
