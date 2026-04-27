package com.example.springrag.infrastructure.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Embedding 服务工厂。
 * <p>
 * 这个工厂类的核心价值不是“省几行 new 代码”，
 * 而是把“创建失败时如何回退”的策略集中放在一起。
 * 这样配置类和业务类都不用关心异常降级细节。
 */
@Component
public class EmbeddingServiceFactory {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceFactory.class);

    /**
     * 创建默认的 embedding 服务。
     *
     * @return 默认实现，当前优先尝试本地 LangChain4j 模型
     */
    public TextEmbeddingService createDefault() {
        return create(LangChain4jLocalEmbeddingService::new);
    }

    /**
     * 使用给定创建器构造 embedding 服务，并在失败时自动降级。
     *
     * @param localSupplier 本地 embedding 服务的创建逻辑
     * @return 创建成功则返回真实实现；失败则回退到简单实现
     */
    TextEmbeddingService create(Supplier<TextEmbeddingService> localSupplier) {
        try {
            return localSupplier.get();
        } catch (Throwable throwable) {
            // 本地模型初始化失败时自动降级，
            // 这样本地开发和测试环境仍然能继续运行。
            log.warn("本地 LangChain4j embedding 模型初始化失败，已回退到简单嵌入实现。", throwable);
            return new SimpleTextEmbeddingService();
        }
    }
}
