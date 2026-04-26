import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import App from "../App";
import * as chatService from "../services/chat";

vi.mock("../services/chat", async () => {
  const actual = await vi.importActual<typeof import("../services/chat")>("../services/chat");
  return {
    ...actual,
    streamChat: vi.fn()
  };
});

describe("ChatWindow", () => {
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
      yield { type: "chunk", content: "根据知识库《guide.md》的内容：" };
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
});
