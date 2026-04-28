# Qwen RAG P0 Design

## Goal

Complete the P0 backend loop for this repository by replacing the placeholder RAG workflow nodes with real DashScope Qwen calls, adding retrieval retry, and validating the full upload -> index -> retrieve -> answer chain in both `inmemory` and `chroma` modes.

## Scope

- Keep the current backend layering and reuse the existing `KnowledgeBaseService`, `RagWorkflowService`, and chat controller flow.
- Integrate DashScope Qwen as the external LLM for question rewriting, retrieval evaluation, and final answer generation.
- Upgrade the workflow from a single-pass graph to a retry-capable loop:
  `Retriever -> Evaluator -> Rewriter/Retry -> Generator/Fallback`.
- Make the LLM integration configurable and safe to disable when no API key is present.
- Verify the end-to-end chain for document upload, indexing, retrieval, and chat answering with `chroma` mode enabled.

## Out Of Scope

- Persistent session state or checkpoint storage.
- Frontend feature work beyond what is needed to keep the existing chat flow working.
- Rich document metadata, advanced chunking, or document management APIs.
- Human handoff strategy redesign.

## Existing Context

The current backend already has:

- a `KnowledgeBaseService` implementation backed by embeddings and an `EmbeddingStore`
- configurable `inmemory` or `chroma` vector storage
- a `RagWorkflowService` built with LangGraph4j
- a `StubChatService` that loads session state, searches the knowledge base, and streams a single answer over SSE

The current P0 gap is that:

- `rewriter` is a pass-through node
- `evaluator` only checks whether `sources` is empty
- `generator` concatenates the first source snippet instead of using an LLM
- the workflow cannot re-run retrieval with a rewritten query

## Architecture

The repository will continue using `LangChain4j` for embeddings and `LangGraph4j` for workflow orchestration. A new DashScope adapter layer will be introduced for chat-model responsibilities only. Retrieval stays in the knowledge-base service so vector store concerns remain isolated from LLM concerns.

The workflow service will become stateful enough to carry:

- the original user question
- the active retrieval query
- the retrieved sources
- retry counters
- evaluation result and reason
- final answer

## Components

### DashScope Configuration

Add a dedicated configuration object under `app.rag.llm` for:

- `enabled`
- `provider`
- `model`
- `api-key`
- `base-url`
- `temperature`
- `timeout-seconds`

This keeps LLM concerns independent from embedding and vector-store settings.

### RAG Retrieval Configuration

Add retrieval workflow settings under `app.rag.retrieval` for:

- `top-k`
- `min-score`
- `max-retries`

This removes hard-coded search parameters from the knowledge-base implementation and allows the workflow to control retry behavior explicitly.

### DashScope Qwen Client

Introduce a thin client responsible for HTTP calls to DashScope compatible chat completions. It will expose workflow-oriented operations:

- rewrite the user question into a retrieval-friendly query
- judge whether retrieved snippets are sufficient to answer the question
- generate the final answer from retrieved context

The rest of the application should not depend on DashScope-specific request/response details.

### Workflow State

Extend the workflow state so nodes can share:

- `question`
- `currentQuery`
- `sources`
- `retryCount`
- `retrievalSatisfied`
- `evaluationReason`
- `answer`

This supports loops without overloading node-local behavior.

## Data Flow

### Happy Path

1. The chat endpoint receives `threadId` and user message.
2. `StubChatService` loads session state and appends the user message.
3. `RagWorkflowService` starts with the original question as the first retrieval query.
4. `retriever` searches the knowledge base using the current query.
5. `evaluator` performs a lightweight rule check and then asks Qwen whether the retrieved context is sufficient.
6. If sufficient, `generator` asks Qwen to answer strictly from the provided context.
7. The workflow returns the answer and the chat service streams it to the client.

### Retry Path

1. Initial retrieval returns weak or empty context.
2. `evaluator` marks retrieval as insufficient.
3. If `retryCount < maxRetries`, the workflow goes to `rewriter`.
4. `rewriter` asks Qwen to transform the question into a better retrieval query.
5. The workflow increments `retryCount` and routes back to `retriever`.
6. If the second evaluation passes, generation proceeds; otherwise the workflow falls back.

### Fallback Path

When retrieval remains insufficient after the configured number of retries, the workflow returns a clear fallback answer explaining that the knowledge base does not contain enough information yet.

## Prompting Rules

### Rewriter

The rewrite prompt should:

- preserve user intent
- expand ambiguous wording into retrieval-friendly keywords
- return a single concise rewritten query
- avoid adding facts not present in the question

### Evaluator

The evaluator prompt should:

- inspect the question and retrieved snippets
- decide whether the snippets are sufficient to answer
- return a structured yes/no style result plus a short reason

To improve robustness, a simple rule pre-check will reject obviously empty retrieval results before calling the model.

### Generator

The generator prompt should:

- answer only from the provided snippets
- cite source titles when possible
- explicitly say the knowledge base is insufficient if context still does not support an answer
- avoid hallucinating missing facts

## Error Handling

- If `app.rag.llm.enabled=false`, the application should still start and tests that do not require real LLM calls should remain runnable.
- If the DashScope client is enabled but required settings are missing, startup should fail with a clear configuration error.
- If a live DashScope request fails during answer generation, the workflow should return a fallback answer rather than crashing the entire chat stream.
- Chroma connection failures should keep using the existing fallback-to-inmemory behavior unless the test explicitly expects a live Chroma run.

## Testing Strategy

### Unit Tests

- Workflow tests should cover:
  - generator branch when retrieval is sufficient
  - rewrite and retry branch when the first retrieval is insufficient
  - fallback branch when retries are exhausted
- DashScope client parsing and validation logic should be tested in isolation where practical.

### Integration Tests

- Existing chat integration should be updated so the workflow can be exercised without real network calls by mocking or stubbing the LLM adapter.
- A dedicated `chroma` profile integration test should verify that document upload, indexing, retrieval, and answer generation all work with a running Chroma container.

## Acceptance Criteria

- A real DashScope Qwen-backed generator replaces the placeholder answer concatenation.
- A real DashScope Qwen-backed rewriter replaces the pass-through node.
- Retrieval evaluation is stronger than `sources.isEmpty()` and can trigger retry behavior.
- The workflow supports `Retriever -> Evaluator -> Rewriter/Retry -> Generator/Fallback`.
- The application can run end-to-end against Chroma and answer from uploaded knowledge.
