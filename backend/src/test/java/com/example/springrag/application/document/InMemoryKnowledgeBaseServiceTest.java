package com.example.springrag.application.document;

import com.example.springrag.domain.chat.SourceReference;
import com.example.springrag.domain.document.DocumentRecord;
import com.example.springrag.infrastructure.document.InMemoryKnowledgeBaseService;
import com.example.springrag.infrastructure.document.SimpleTextEmbeddingService;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryKnowledgeBaseServiceTest {

    @Test
    void shouldStoreDocumentAndRetrieveRelevantChunk() {
        InMemoryKnowledgeBaseService knowledgeBaseService =
                new InMemoryKnowledgeBaseService(new InMemoryEmbeddingStore<>(), new SimpleTextEmbeddingService());

        DocumentRecord record = knowledgeBaseService.store("guide.md", """
                Warranty period is two years.
                Contact online support before repair.
                """.getBytes(StandardCharsets.UTF_8));

        List<SourceReference> references = knowledgeBaseService.search("warranty period");

        assertThat(record.status()).isEqualTo("INDEXED");
        assertThat(references).isNotEmpty();
        assertThat(references.get(0).snippet()).contains("Warranty period is two years");
    }

    @Test
    void shouldReturnAtMostThreeRelevantChunks() {
        InMemoryKnowledgeBaseService knowledgeBaseService =
                new InMemoryKnowledgeBaseService(new InMemoryEmbeddingStore<>(), new SimpleTextEmbeddingService());

        knowledgeBaseService.store("guide-a.md", """
                Warranty period is two years.
                Warranty extension is available for enterprise users.
                Warranty applies to the main device only.
                Warranty claims require a serial number.
                """.getBytes(StandardCharsets.UTF_8));

        List<SourceReference> references = knowledgeBaseService.search("warranty");

        assertThat(references).hasSize(3);
    }
}
