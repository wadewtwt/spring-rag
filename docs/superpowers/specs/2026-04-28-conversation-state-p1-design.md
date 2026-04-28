# Conversation State P1 Design

## Goal

Complete the first conversation-state-focused P1 slice by replacing in-memory session storage with database persistence and making the chat workflow consume persisted conversation history instead of treating `threadId` as a frontend-only marker.

## Scope

- Persist `SessionState` by `threadId`.
- Store restorable session snapshots including message history, last retrieved sources, handoff state, and failure counters.
- Pass recent history into retrieval rewrite, evaluation, and answer generation so follow-up questions can use prior turns.
- Define clear failure increment and reset rules inside the chat flow.
- Keep the existing `/api/chat/stream` request shape unchanged.

## Out Of Scope

- New frontend APIs for listing or resuming threads.
- Human handoff channel integrations such as webhook or email.
- Multi-tenant isolation and authorization.
- Replacing the current SSE contract.

## Existing Context

The current flow stores `SessionState` in `InMemorySessionStateRepository`, keyed by `threadId`. That lets the backend append messages during a single process lifetime, but the state disappears on restart and the workflow itself does not consume the stored history. The result is that multi-turn chat only appears to exist because the frontend keeps sending the same `threadId`.

## Architecture

This slice will introduce a database-backed session repository while keeping `SessionStateRepository` as the application boundary. The persistence model will store one row per thread with a serialized snapshot payload so the current domain model can evolve without scattering state across many tables too early.

`StubChatService` will remain the orchestration point for loading session state, appending the user turn, executing the workflow, updating counters, and saving the new snapshot. `RagWorkflowService` will gain an overload that accepts recent chat history so retrieval rewriting and answer generation can use the latest turns as context.

## Persistence Model

Store one persistent session record with:

- `threadId`
- serialized `messages`
- serialized `lastSources`
- `handoffSuggested`
- `consecutiveFailures`
- timestamps for create/update bookkeeping

The implementation should default to an embedded H2 datasource so local development and tests work without extra setup. The row format should be JSON-backed to keep the persistence schema narrow while still restoring the current domain object faithfully.

## Restorable Session Snapshot

`SessionState` will continue to be the application-facing session aggregate, but it should gain a factory for rebuilding itself from a stored snapshot and expose immutable snapshot data for serialization. This keeps the persistence adapter responsible for JSON mapping while the domain still defines what a valid session contains.

The snapshot must include enough information to survive process restarts:

- ordered message history with timestamps
- latest source list
- handoff suggestion flag
- failure counter

## Conversational Context

The workflow should stop operating on the latest user message in isolation. Instead, it should derive a small recent window from session history, such as the last few user/assistant turns, and pass that context to the LLM gateway methods used for rewrite and answer generation.

This slice should stay conservative: history is used to improve follow-up understanding, not to redesign the whole graph state machine. The existing API shape and main workflow graph remain intact.

## Failure Rules

- Successful answer generation resets `consecutiveFailures` to `0`.
- Workflow fallback increments `consecutiveFailures` by `1`.
- Workflow execution exceptions increment `consecutiveFailures` by `1` before surfacing the error.
- Handoff recommendation reads the persisted counter after applying the latest turn result.

This makes the rule deterministic and restart-safe.

## Testing Strategy

Add test coverage for:

- repository persistence and reload from the database
- restoration of messages, last sources, handoff flag, and failure count
- chat flow preserving state across multiple requests with the same `threadId`
- follow-up questions using previous turns as workflow context
- failure counter increment and reset behavior

## Acceptance Criteria

- Session state survives backend restarts because it is stored in the database.
- `threadId` maps to a restorable session snapshot, not an in-memory object only.
- Follow-up questions can use recent history when generating answers.
- Failure counters behave consistently across requests and restarts.
- Existing frontend chat requests continue to work without contract changes.
