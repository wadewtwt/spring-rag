package com.example.springrag.infrastructure.document;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class EmbeddingStoreFactory {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingStoreFactory.class);

    public EmbeddingStore<TextSegment> createChromaOrFallback(Supplier<EmbeddingStore<TextSegment>> chromaSupplier) {
        try {
            return chromaSupplier.get();
        } catch (Throwable throwable) {
            // Chroma 不可用时自动回退，确保开发和测试环境不会被外部依赖卡住。
            log.warn("Chroma embedding store 初始化失败，已回退到内存向量存储。", throwable);
            return new InMemoryEmbeddingStore<>();
        }
    }
}
