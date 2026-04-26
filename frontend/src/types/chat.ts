export type ChatRequest = {
  threadId: string;
  message: string;
};

export type SourceReferenceView = {
  title: string;
  snippet: string;
};

export type StreamEvent =
  | { type: "chunk"; content: string }
  | { type: "sources"; sources: SourceReferenceView[] }
  | { type: "handoff"; message: string }
  | { type: "complete" };
