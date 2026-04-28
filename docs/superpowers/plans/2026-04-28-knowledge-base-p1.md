# Knowledge Base P1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the backend knowledge-base ingestion flow to support real `txt`/`md`/`pdf` parsing, paragraph-aware chunking with overlap, richer chunk metadata, and retrieval settings driven by `RagProperties`.

**Architecture:** Keep `InMemoryKnowledgeBaseService` as the main orchestration point, but extend it with focused helper methods for file parsing, chunk building, and metadata creation. Expand the existing domain DTOs so indexing and retrieval can expose ingestion context without changing the upload endpoint shape.

**Tech Stack:** Spring Boot 3, Maven, JUnit 5, AssertJ, LangChain4j, Apache PDFBox

---

### Task 1: Add coverage for richer document records and source metadata

**Files:**
- Modify: `backend/src/test/java/com/example/springrag/application/document/InMemoryKnowledgeBaseServiceTest.java`
- Modify: `backend/src/main/java/com/example/springrag/domain/document/DocumentRecord.java`
- Modify: `backend/src/main/java/com/example/springrag/domain/chat/SourceReference.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void shouldExposeDocumentAndChunkMetadataAfterIndexing() {
    InMemoryKnowledgeBaseService knowledgeBaseService =
            new InMemoryKnowledgeBaseService(new InMemoryEmbeddingStore<>(), new SimpleTextEmbeddingService(), retrievalProperties(3, 0.15));

    DocumentRecord record = knowledgeBaseService.store("guide.txt", """
            Warranty period is two years.

            Contact online support before repair.
            """.getBytes(StandardCharsets.UTF_8));

    List<SourceReference> references = knowledgeBaseService.search("warranty period");

    assertThat(record.status()).isEqualTo("INDEXED");
    assertThat(record.uploadedAt()).isNotNull();
    assertThat(record.chunkCount()).isGreaterThan(0);
    assertThat(references).isNotEmpty();
    assertThat(references.get(0).documentId()).isEqualTo(record.documentId());
    assertThat(references.get(0).chunkIndex()).isNotNull();
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd -Dtest=InMemoryKnowledgeBaseServiceTest#shouldExposeDocumentAndChunkMetadataAfterIndexing test`

Expected: FAIL because `DocumentRecord` and `SourceReference` do not yet expose the asserted fields, and `InMemoryKnowledgeBaseService` does not have the new constructor.

- [ ] **Step 3: Write minimal implementation**

```java
public record DocumentRecord(
        String documentId,
        String fileName,
        String status,
        Instant uploadedAt,
        int chunkCount
) {
}
```

```java
public record SourceReference(
        String title,
        String snippet,
        String documentId,
        Integer chunkIndex,
        Integer pageNumber
) implements Serializable {
}
```

```java
private SourceReference toSourceReference(TextSegment segment) {
    String fileName = segment.metadata().getString("fileName");
    return new SourceReference(
            fileName == null ? "unknown" : fileName,
            segment.text(),
            segment.metadata().getString("documentId"),
            segment.metadata().getInteger("chunkIndex"),
            segment.metadata().getInteger("pageNumber"));
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\mvnw.cmd -Dtest=InMemoryKnowledgeBaseServiceTest#shouldExposeDocumentAndChunkMetadataAfterIndexing test`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/test/java/com/example/springrag/application/document/InMemoryKnowledgeBaseServiceTest.java backend/src/main/java/com/example/springrag/domain/document/DocumentRecord.java backend/src/main/java/com/example/springrag/domain/chat/SourceReference.java
git commit -m "feat: expose knowledge base metadata"
```

### Task 2: Make retrieval settings runtime-configurable

**Files:**
- Modify: `backend/src/test/java/com/example/springrag/application/document/InMemoryKnowledgeBaseServiceTest.java`
- Modify: `backend/src/main/java/com/example/springrag/infrastructure/document/InMemoryKnowledgeBaseService.java`
- Modify: `backend/src/main/java/com/example/springrag/config/RagProperties.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void shouldRespectConfiguredTopK() {
    InMemoryKnowledgeBaseService knowledgeBaseService =
            new InMemoryKnowledgeBaseService(new InMemoryEmbeddingStore<>(), new SimpleTextEmbeddingService(), retrievalProperties(2, 0.0));

    knowledgeBaseService.store("guide.txt", """
            warranty first

            warranty second

            warranty third
            """.getBytes(StandardCharsets.UTF_8));

    List<SourceReference> references = knowledgeBaseService.search("warranty");

    assertThat(references).hasSize(2);
}

@Test
void shouldRespectConfiguredMinScore() {
    InMemoryKnowledgeBaseService knowledgeBaseService =
            new InMemoryKnowledgeBaseService(new InMemoryEmbeddingStore<>(), new SimpleTextEmbeddingService(), retrievalProperties(3, 0.95));

    knowledgeBaseService.store("guide.txt", """
            Warranty period is two years.
            """.getBytes(StandardCharsets.UTF_8));

    List<SourceReference> references = knowledgeBaseService.search("warranty");

    assertThat(references).isEmpty();
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\mvnw.cmd -Dtest=InMemoryKnowledgeBaseServiceTest#shouldRespectConfiguredTopK,InMemoryKnowledgeBaseServiceTest#shouldRespectConfiguredMinScore test`

Expected: FAIL because search still uses hard-coded `maxResults(3)` and `minScore(0.15)`.

- [ ] **Step 3: Write minimal implementation**

```java
private final RagProperties.Retrieval retrievalProperties;

public InMemoryKnowledgeBaseService(EmbeddingStore<TextSegment> embeddingStore,
                                    TextEmbeddingService embeddingService,
                                    RagProperties ragProperties) {
    this(embeddingStore, embeddingService, ragProperties.getRetrieval());
}

InMemoryKnowledgeBaseService(EmbeddingStore<TextSegment> embeddingStore,
                             TextEmbeddingService embeddingService,
                             RagProperties.Retrieval retrievalProperties) {
    this.embeddingStore = embeddingStore;
    this.embeddingService = embeddingService;
    this.retrievalProperties = retrievalProperties;
}
```

```java
EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
        .queryEmbedding(queryEmbedding)
        .maxResults(retrievalProperties.getTopK())
        .minScore(retrievalProperties.getMinScore())
        .build();
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\mvnw.cmd -Dtest=InMemoryKnowledgeBaseServiceTest#shouldRespectConfiguredTopK,InMemoryKnowledgeBaseServiceTest#shouldRespectConfiguredMinScore test`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/test/java/com/example/springrag/application/document/InMemoryKnowledgeBaseServiceTest.java backend/src/main/java/com/example/springrag/infrastructure/document/InMemoryKnowledgeBaseService.java backend/src/main/java/com/example/springrag/config/RagProperties.java
git commit -m "feat: configure knowledge base retrieval"
```

### Task 3: Replace line-based chunking with paragraph-aware chunking

**Files:**
- Modify: `backend/src/test/java/com/example/springrag/application/document/InMemoryKnowledgeBaseServiceTest.java`
- Modify: `backend/src/main/java/com/example/springrag/infrastructure/document/InMemoryKnowledgeBaseService.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void shouldMergeShortParagraphsIntoLargerChunks() {
    InMemoryKnowledgeBaseService knowledgeBaseService =
            new InMemoryKnowledgeBaseService(new InMemoryEmbeddingStore<>(), new SimpleTextEmbeddingService(), retrievalProperties(5, 0.0));

    DocumentRecord record = knowledgeBaseService.store("guide.txt", """
            Warranty period is two years.

            Contact online support before repair.

            Keep your invoice for service claims.
            """.getBytes(StandardCharsets.UTF_8));

    assertThat(record.chunkCount()).isLessThan(3);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd -Dtest=InMemoryKnowledgeBaseServiceTest#shouldMergeShortParagraphsIntoLargerChunks test`

Expected: FAIL because each non-empty line is still stored as an independent chunk.

- [ ] **Step 3: Write minimal implementation**

```java
private List<ChunkCandidate> splitIntoChunks(String text, String fileType) {
    List<String> paragraphs = splitIntoParagraphs(text);
    List<ChunkCandidate> chunks = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    int currentParagraphStart = 0;

    for (int index = 0; index < paragraphs.size(); index++) {
        String paragraph = paragraphs.get(index);
        if (current.isEmpty()) {
            currentParagraphStart = index;
        }

        if (!current.isEmpty() && current.length() + 2 + paragraph.length() > 600) {
            chunks.add(new ChunkCandidate(current.toString(), currentParagraphStart, null));
            current = new StringBuilder(overlapTail(current.toString(), 120));
        }

        if (!current.isEmpty()) {
            current.append("\n\n");
        }
        current.append(paragraph);
    }

    if (!current.isEmpty()) {
        chunks.add(new ChunkCandidate(current.toString(), currentParagraphStart, null));
    }
    return chunks;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\mvnw.cmd -Dtest=InMemoryKnowledgeBaseServiceTest#shouldMergeShortParagraphsIntoLargerChunks test`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/test/java/com/example/springrag/application/document/InMemoryKnowledgeBaseServiceTest.java backend/src/main/java/com/example/springrag/infrastructure/document/InMemoryKnowledgeBaseService.java
git commit -m "feat: improve knowledge base chunking"
```

### Task 4: Support normalized Markdown parsing

**Files:**
- Modify: `backend/src/test/java/com/example/springrag/application/document/InMemoryKnowledgeBaseServiceTest.java`
- Modify: `backend/src/main/java/com/example/springrag/infrastructure/document/InMemoryKnowledgeBaseService.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void shouldParseMarkdownIntoCleanSearchableText() {
    InMemoryKnowledgeBaseService knowledgeBaseService =
            new InMemoryKnowledgeBaseService(new InMemoryEmbeddingStore<>(), new SimpleTextEmbeddingService(), retrievalProperties(3, 0.0));

    knowledgeBaseService.store("guide.md", """
            # Warranty Guide

            - Warranty period is two years.
            - Contact online support before repair.
            """.getBytes(StandardCharsets.UTF_8));

    List<SourceReference> references = knowledgeBaseService.search("online support");

    assertThat(references).isNotEmpty();
    assertThat(references.get(0).snippet()).contains("Warranty Guide");
    assertThat(references.get(0).snippet()).doesNotContain("#");
    assertThat(references.get(0).snippet()).doesNotContain("- ");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd -Dtest=InMemoryKnowledgeBaseServiceTest#shouldParseMarkdownIntoCleanSearchableText test`

Expected: FAIL because Markdown syntax is still indexed verbatim.

- [ ] **Step 3: Write minimal implementation**

```java
private String parseDocument(String fileName, byte[] content) {
    String fileType = detectFileType(fileName);
    return switch (fileType) {
        case "md" -> normalizeMarkdown(asUtf8Text(content));
        case "txt" -> normalizePlainText(asUtf8Text(content));
        case "pdf" -> extractPdfText(content);
        default -> throw new IllegalArgumentException("Unsupported file type: " + fileType);
    };
}

private String normalizeMarkdown(String text) {
    return text.replaceAll("(?m)^#{1,6}\\s*", "")
            .replaceAll("(?m)^[-*+]\\s+", "")
            .replaceAll("(?m)^\\d+\\.\\s+", "")
            .trim();
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\mvnw.cmd -Dtest=InMemoryKnowledgeBaseServiceTest#shouldParseMarkdownIntoCleanSearchableText test`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/test/java/com/example/springrag/application/document/InMemoryKnowledgeBaseServiceTest.java backend/src/main/java/com/example/springrag/infrastructure/document/InMemoryKnowledgeBaseService.java
git commit -m "feat: normalize markdown knowledge files"
```

### Task 5: Add real PDF parsing with page metadata

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/test/java/com/example/springrag/application/document/InMemoryKnowledgeBaseServiceTest.java`
- Modify: `backend/src/main/java/com/example/springrag/infrastructure/document/InMemoryKnowledgeBaseService.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void shouldExtractPdfTextAndExposePageMetadata() throws IOException {
    InMemoryKnowledgeBaseService knowledgeBaseService =
            new InMemoryKnowledgeBaseService(new InMemoryEmbeddingStore<>(), new SimpleTextEmbeddingService(), retrievalProperties(3, 0.0));

    byte[] pdf = samplePdf("Warranty period is two years.");

    knowledgeBaseService.store("guide.pdf", pdf);

    List<SourceReference> references = knowledgeBaseService.search("warranty period");

    assertThat(references).isNotEmpty();
    assertThat(references.get(0).snippet()).contains("Warranty period is two years.");
    assertThat(references.get(0).pageNumber()).isEqualTo(1);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd -Dtest=InMemoryKnowledgeBaseServiceTest#shouldExtractPdfTextAndExposePageMetadata test`

Expected: FAIL because PDF bytes are not parsed into text and no page metadata is captured.

- [ ] **Step 3: Write minimal implementation**

```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.3</version>
</dependency>
```

```java
private ParsedDocument parsePdf(byte[] content) {
    try (PDDocument document = Loader.loadPDF(content)) {
        PDFTextStripper stripper = new PDFTextStripper();
        List<PageSlice> pages = new ArrayList<>();
        for (int page = 1; page <= document.getNumberOfPages(); page++) {
            stripper.setStartPage(page);
            stripper.setEndPage(page);
            pages.add(new PageSlice(page, stripper.getText(document).trim()));
        }
        return ParsedDocument.fromPages("pdf", pages);
    } catch (IOException ex) {
        throw new IllegalArgumentException("Failed to parse PDF document", ex);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\mvnw.cmd -Dtest=InMemoryKnowledgeBaseServiceTest#shouldExtractPdfTextAndExposePageMetadata test`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/pom.xml backend/src/test/java/com/example/springrag/application/document/InMemoryKnowledgeBaseServiceTest.java backend/src/main/java/com/example/springrag/infrastructure/document/InMemoryKnowledgeBaseService.java
git commit -m "feat: parse pdf knowledge files"
```

### Task 6: Verify upload and chat regression coverage

**Files:**
- Modify: `backend/src/test/java/com/example/springrag/api/document/DocumentControllerTest.java`
- Modify: `backend/src/test/java/com/example/springrag/api/chat/ChatRagIntegrationTest.java`
- Modify: `backend/src/test/resources/application.yml`

- [ ] **Step 1: Write the failing regression assertions**

```java
andExpect(jsonPath("$.chunkCount").value(greaterThan(0)))
andExpect(jsonPath("$.uploadedAt").isNotEmpty());
```

```java
assertThat(body).contains("\"pageNumber\":1");
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\mvnw.cmd -Dtest=DocumentControllerTest,ChatRagIntegrationTest test`

Expected: FAIL because the response shape and source payloads do not yet include the richer metadata.

- [ ] **Step 3: Write minimal implementation**

```java
return ResponseEntity.ok(knowledgeBaseService.store(file.getOriginalFilename(), file.getBytes()));
```

```java
private void sendSources(SseEmitter emitter, List<SourceReference> sources) throws IOException {
    emitter.send(SseEmitter.event().name("sources").data(sources));
}
```

Update only the DTO serialization fallout required by the earlier domain changes; do not redesign controller contracts.

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\mvnw.cmd -Dtest=DocumentControllerTest,ChatRagIntegrationTest test`

Expected: PASS

- [ ] **Step 5: Run the focused backend suite**

Run: `.\mvnw.cmd -Dtest=InMemoryKnowledgeBaseServiceTest,DocumentControllerTest,ChatRagIntegrationTest test`

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add backend/src/test/java/com/example/springrag/api/document/DocumentControllerTest.java backend/src/test/java/com/example/springrag/api/chat/ChatRagIntegrationTest.java backend/src/test/resources/application.yml
git add backend/src/main/java/com/example/springrag/api/document/DocumentController.java backend/src/main/java/com/example/springrag/infrastructure/chat/StubChatService.java
git commit -m "test: cover knowledge base p1 regression flow"
```
