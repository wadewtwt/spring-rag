package com.example.springrag.infrastructure.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingServiceConfiguration {

    @Bean
    public TextEmbeddingService textEmbeddingService(
            @Value("${app.rag.embedding.mode:local}") String mode,
            EmbeddingServiceFactory factory) {
        // 测试环境可以切到 simple，开发环境默认优先本地模型。
        if ("simple".equalsIgnoreCase(mode)) {
            return new SimpleTextEmbeddingService();
        }
        return factory.createDefault();
    }
}
