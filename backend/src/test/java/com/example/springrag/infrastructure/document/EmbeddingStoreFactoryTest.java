package com.example.springrag.infrastructure.document;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingStoreFactoryTest {

    @Test
    void shouldFallbackToInMemoryEmbeddingStoreWhenChromaCreationFails() {
        EmbeddingStoreFactory factory = new EmbeddingStoreFactory();

        EmbeddingStore<TextSegment> store = factory.createChromaOrFallback(() -> {
            throw new IllegalStateException("chroma init failed");
        });

        assertThat(store).isInstanceOf(InMemoryEmbeddingStore.class);
    }
}
