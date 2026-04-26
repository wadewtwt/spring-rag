package com.example.springrag.infrastructure.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class EmbeddingServiceFactory {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceFactory.class);

    public TextEmbeddingService createDefault() {
        return create(LangChain4jLocalEmbeddingService::new);
    }

    TextEmbeddingService create(Supplier<TextEmbeddingService> localSupplier) {
        try {
            return localSupplier.get();
        } catch (Throwable throwable) {
            // 本地模型加载失败时自动降级，保证开发环境和测试环境都还能继续工作。
            log.warn("本地 LangChain4j embedding 模型初始化失败，已回退到简单嵌入实现。", throwable);
            return new SimpleTextEmbeddingService();
        }
    }
}
