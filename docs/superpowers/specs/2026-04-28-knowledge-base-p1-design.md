# Knowledge Base P1 Design

## Goal

Complete the first knowledge-base-focused P1 slice by upgrading the existing backend upload and retrieval chain from raw byte decoding plus line-based chunking into a more usable document ingestion flow with real file parsing, better chunking, richer metadata, and configurable retrieval parameters.

## Scope

- Keep the current backend architecture centered on `KnowledgeBaseService` and `InMemoryKnowledgeBaseService`.
- Upgrade document ingestion for `txt`, `md`, and `pdf` files.
- Replace line-based chunking with paragraph-aware chunking plus small overlapping windows.
- Attach richer metadata to documents and chunks when indexing.
- Make retrieval `top-k` and minimum score use real configuration instead of hard-coded values.
- Preserve the current upload API shape so the frontend does not need changes in this slice.

## Out Of Scope

- Document list, delete, or reindex management APIs.
- Database persistence for document records or chunk metadata.
- Tenant or user authentication flows.
- Changes to chat session persistence or human handoff behavior.
- Full parser framework extraction into many new backend components.

## Existing Context

The current implementation stores uploaded files by:

1. decoding all bytes as UTF-8 text
2. splitting the text by line
3. writing each non-empty line into the embedding store with minimal metadata

This creates three problems:

- `pdf` files are not truly parsed
- Markdown structure is treated as plain text noise
- line-level chunks are often too small and lose context

The current search path also hard-codes `maxResults(3)` and `minScore(0.15)`, which means retrieval configuration exists conceptually in the repo but does not fully drive runtime behavior.

## Architecture

This slice stays intentionally conservative. `InMemoryKnowledgeBaseService` remains the main orchestrator for upload parsing, chunk creation, embedding, and search. Supporting logic may be extracted only as thin helpers if that makes the service readable, but the design does not introduce a new ingestion subsystem.

Document ingestion will become a four-stage flow:

1. detect file type from filename
2. parse bytes into normalized document text
3. split normalized text into paragraph-based chunks with overlap
4. index chunks with richer metadata

Search will keep using the existing embedding store abstraction, but request parameters will be sourced from `RagProperties`.

## File Parsing

### TXT

`txt` files will continue using UTF-8 decoding, but the implementation should normalize line endings and trim obvious binary noise failure cases when possible.

### Markdown

`md` files will be parsed as text with lightweight normalization:

- preserve heading content
- preserve paragraph content
- preserve list item text
- strip Markdown control characters that do not add retrieval value when they appear as formatting markers

The goal is not to render Markdown, only to convert it into cleaner retrieval text.

### PDF

`pdf` files will use a real PDF text extraction library so uploaded PDFs are no longer interpreted as raw bytes. The parser should extract text page by page so page numbers can be recorded in chunk metadata when available.

If PDF parsing fails, the upload should fail clearly rather than silently indexing corrupted text.

## Chunking Strategy

The current line-based splitter will be replaced with a paragraph-aware strategy:

1. split normalized text into paragraphs using blank lines as strong boundaries
2. trim empty paragraphs
3. merge adjacent short paragraphs into a chunk until a target character budget is reached
4. if a paragraph is too large on its own, split it into fixed-size windows
5. carry a small overlap between neighboring windows to avoid context loss

This strategy is intentionally simple enough to keep inside the current service while producing better retrieval units than one-line chunks.

### Chunk Metadata

Each indexed chunk should carry:

- `documentId`
- `fileName`
- `chunkIndex`
- `paragraphIndex`
- `uploadedAt`
- `fileType`
- `pageNumber` when the source is PDF and the page is known

These metadata fields should be written into `TextSegment.metadata()` so future source rendering and document management work can reuse them.

## Domain Object Changes

### DocumentRecord

`DocumentRecord` should continue returning the current basics and expand to include:

- upload timestamp
- indexed chunk count

This gives callers a better acknowledgment of what was indexed without adding a database layer.

### SourceReference

`SourceReference` should expand from title plus snippet into a richer retrieval result object that can carry:

- display title or file name
- snippet text
- `documentId`
- `chunkIndex`
- `pageNumber` when present

This prepares the backend for later frontend source highlighting without requiring that UI work in this slice.

## Retrieval Configuration

The knowledge-base search method should stop hard-coding retrieval parameters. `RagProperties` should expose runtime values for:

- `topK`
- `minScore`

Search requests should read these values directly so different profiles can tune retrieval behavior without code changes.

## Error Handling

- Unsupported file types should fail fast with a clear validation error.
- Empty extracted text should fail indexing rather than creating zero useful chunks.
- PDF parse failures should surface as upload errors.
- Search should keep returning an empty list when no chunk meets the configured threshold.

## Testing Strategy

### Unit Tests

Add or update backend tests to cover:

- TXT parsing and indexing still works
- Markdown parsing removes formatting noise while preserving content
- PDF parsing extracts searchable text
- chunking produces larger semantic chunks than single-line splitting
- metadata is present in indexed segments and mapped back into `SourceReference`
- retrieval respects configured `topK`
- retrieval respects configured `minScore`

### Integration Confidence

The existing upload-plus-chat integration path should continue passing after these changes so the knowledge-base enhancement does not break the current RAG flow.

## Acceptance Criteria

- Uploading `txt`, `md`, and `pdf` files produces real searchable text.
- Indexed chunks are paragraph-aware and use overlap instead of pure line splitting.
- Chunk metadata includes document identity and ingestion context.
- Search request `top-k` and minimum score are no longer hard-coded.
- Existing upload endpoint remains compatible with the current frontend.
