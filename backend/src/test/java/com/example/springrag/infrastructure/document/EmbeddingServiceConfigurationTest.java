package com.example.springrag.infrastructure.document;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingServiceConfigurationTest {

    @Test
    void shouldCreateSimpleEmbeddingServiceWhenModeIsSimple() {
        EmbeddingServiceConfiguration configuration = new EmbeddingServiceConfiguration();

        TextEmbeddingService service = configuration.textEmbeddingService("simple", new EmbeddingServiceFactory());

        assertThat(service).isInstanceOf(SimpleTextEmbeddingService.class);
    }
}
