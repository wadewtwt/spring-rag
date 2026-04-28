import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import App from "../App";
import * as chatService from "../services/chat";
import * as documentService from "../services/document";

vi.mock("../services/chat", async () => {
  const actual = await vi.importActual<typeof import("../services/chat")>("../services/chat");
  return {
    ...actual,
    streamChat: vi.fn()
  };
});

vi.mock("../services/document", async () => {
  const actual = await vi.importActual<typeof import("../services/document")>("../services/document");
  return {
    ...actual,
    uploadDocument: vi.fn()
  };
});

describe("ChatWindow", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("creates and displays thread id on first render", () => {
    render(<App />);
    expect(screen.getByText(/线程 ID/)).toBeInTheDocument();
  });

  it("renders source placeholder area", () => {
    render(<App />);
    expect(screen.getByText("参考来源")).toBeInTheDocument();
  });

  it("renders returned source cards after streaming", async () => {
    vi.mocked(chatService.streamChat).mockImplementation(async function* () {
      yield { type: "chunk", content: "Answer from guide.md" };
      yield {
        type: "sources",
        sources: [{ title: "guide.md", snippet: "Warranty period is two years." }]
      };
      yield { type: "complete" };
    });

    render(<App />);

    fireEvent.change(screen.getByPlaceholderText("请输入你的问题，例如：设备保修多久？"), {
      target: { value: "warranty period" }
    });
    fireEvent.click(screen.getByRole("button", { name: "发送消息" }));

    await waitFor(() => {
      expect(screen.getByText("guide.md")).toBeInTheDocument();
      expect(screen.getByText("Warranty period is two years.")).toBeInTheDocument();
    });
  });

  it("uploads a document and shows success feedback", async () => {
    vi.mocked(documentService.uploadDocument).mockResolvedValue({
      documentId: "doc-1",
      fileName: "guide.txt",
      status: "INDEXED"
    });

    render(<App />);

    const file = new File(["warranty period is two years"], "guide.txt", { type: "text/plain" });
    fireEvent.change(screen.getByLabelText("Select document"), {
      target: { files: [file] }
    });
    fireEvent.click(screen.getByRole("button", { name: "Upload document" }));

    await waitFor(() => {
      expect(documentService.uploadDocument).toHaveBeenCalledWith(file);
      expect(screen.getByText("Uploaded and indexed: guide.txt")).toBeInTheDocument();
      expect(screen.getByText("Uploaded in this session")).toBeInTheDocument();
      expect(screen.getByText("guide.txt")).toBeInTheDocument();
    });
  });

  it("shows upload error feedback without removing chat area", async () => {
    vi.mocked(documentService.uploadDocument).mockRejectedValue(new Error("upload failed"));

    render(<App />);

    const file = new File(["bad"], "broken.txt", { type: "text/plain" });
    fireEvent.change(screen.getByLabelText("Select document"), {
      target: { files: [file] }
    });
    fireEvent.click(screen.getByRole("button", { name: "Upload document" }));

    await waitFor(() => {
      expect(screen.getByText("Document upload failed. Please retry after checking the backend service.")).toBeInTheDocument();
      expect(screen.getByText("对话窗口")).toBeInTheDocument();
    });
  });
});
