package com.example.springrag.infrastructure.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Embedding 服务的 Spring 配置类。
 * <p>
 * 它的职责是根据配置项决定当前应该向 Spring 容器注册哪种
 * {@link TextEmbeddingService} 实现。
 * <p>
 * 这类类通常会出现在 infrastructure 层，
 * 因为它负责把“配置 + 具体技术实现”装配成可注入的 Bean。
 */
@Configuration
public class EmbeddingServiceConfiguration {

    /**
     * 根据配置决定使用哪种文本向量化实现。
     *
     * @param mode embedding 模式，例如 local 或 simple
     * @param factory 用来创建默认 embedding 服务的工厂
     * @return 可供其他 Bean 注入使用的文本向量化服务
     */
    @Bean
    public TextEmbeddingService textEmbeddingService(
            @Value("${app.rag.embedding.mode:local}") String mode,
            EmbeddingServiceFactory factory) {
        // 测试环境可以切到 simple；
        // 开发环境默认优先尝试本地模型。
        if ("simple".equalsIgnoreCase(mode)) {
            return new SimpleTextEmbeddingService();
        }
        return factory.createDefault();
    }
}
