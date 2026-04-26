import { render, screen } from "@testing-library/react";
import App from "./App";

describe("App", () => {
  it("renders application title", () => {
    render(<App />);
    expect(screen.getByText("Spring RAG 客服控制台")).toBeInTheDocument();
  });
});
