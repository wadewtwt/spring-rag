package com.example.springrag.infrastructure.document;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingStoreConfigurationTest {

    @Test
    void shouldCreateInMemoryEmbeddingStoreWhenModeIsInMemory() {
        EmbeddingStoreConfiguration configuration = new EmbeddingStoreConfiguration();

        EmbeddingStore<TextSegment> store = configuration.embeddingStore(
                "inmemory",
                "http://localhost:8000",
                "v2",
                "default_tenant",
                "default_database",
                "spring-rag",
                new EmbeddingStoreFactory()
        );

        assertThat(store).isInstanceOf(InMemoryEmbeddingStore.class);
    }
}
