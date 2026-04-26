package com.example.springrag.infrastructure.document;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingServiceFactoryTest {

    @Test
    void shouldFallbackToSimpleEmbeddingServiceWhenLocalModelCreationFails() {
        EmbeddingServiceFactory factory = new EmbeddingServiceFactory();

        TextEmbeddingService service = factory.create(() -> {
            throw new IllegalStateException("model init failed");
        });

        assertThat(service).isInstanceOf(SimpleTextEmbeddingService.class);
    }
}
