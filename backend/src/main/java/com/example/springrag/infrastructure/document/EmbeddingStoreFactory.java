package com.example.springrag.infrastructure.document;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 向量存储工厂。
 * <p>
 * 它负责把“优先创建外部向量库，失败则自动降级”的逻辑封装起来，
 * 让配置类保持简洁，也让降级策略更集中。
 */
@Component
public class EmbeddingStoreFactory {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingStoreFactory.class);

    /**
     * 创建 Chroma 向量存储，并在失败时回退到内存实现。
     *
     * @param chromaSupplier 构造 Chroma store 的逻辑
     * @return 成功时返回 Chroma store，失败时返回内存 store
     */
    public EmbeddingStore<TextSegment> createChromaOrFallback(
            Supplier<EmbeddingStore<TextSegment>> chromaSupplier) {
        try {
            return chromaSupplier.get();
        } catch (Throwable throwable) {
            // Chroma 不可用时自动回退，
            // 保证开发和测试环境不会因为外部依赖失败而完全不可用。
            log.warn("Chroma embedding store 初始化失败，已回退到内存向量存储。", throwable);
            return new InMemoryEmbeddingStore<>();
        }
    }
}
