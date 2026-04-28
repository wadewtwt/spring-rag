package com.example.springrag.infrastructure.document;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaApiVersion;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量存储的 Spring 配置类。
 * <p>
 * 它的作用和 EmbeddingServiceConfiguration 很类似：
 * 根据配置决定当前项目使用哪种向量存储实现。
 * <p>
 * 当前支持两种模式：
 * 1. inmemory：内存向量库，适合演示和测试
 * 2. chroma：外部 Chroma 向量库，适合更真实的联调场景
 */
@Configuration
public class EmbeddingStoreConfiguration {

    /**
     * 创建当前项目要使用的向量存储 Bean。
     *
     * @param mode 存储模式
     * @param baseUrl Chroma 服务地址
     * @param tenant Chroma tenant
     * @param database Chroma database
     * @param collection Chroma collection 名称
     * @param factory 负责处理创建失败回退逻辑的工厂
     * @return 可注入的向量存储实现
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(
            @Value("${app.rag.store.mode:inmemory}") String mode,
            @Value("${app.rag.store.chroma.base-url:http://localhost:8000}") String baseUrl,
            @Value("${app.rag.store.chroma.api-version:v2}") String apiVersion,
            @Value("${app.rag.store.chroma.tenant:default_tenant}") String tenant,
            @Value("${app.rag.store.chroma.database:default_database}") String database,
            @Value("${app.rag.store.chroma.collection:spring-rag}") String collection,
            EmbeddingStoreFactory factory) {
        if ("chroma".equalsIgnoreCase(mode)) {
            return factory.createChromaOrFallback(() -> ChromaEmbeddingStore.builder()
                    .apiVersion(resolveApiVersion(apiVersion))
                    .baseUrl(baseUrl)
                    .tenantName(tenant)
                    .databaseName(database)
                    .collectionName(collection)
                    .build());
        }
        return new InMemoryEmbeddingStore<>();
    }

    private ChromaApiVersion resolveApiVersion(String apiVersion) {
        if ("v1".equalsIgnoreCase(apiVersion)) {
            return ChromaApiVersion.V1;
        }
        return ChromaApiVersion.V2;
    }
}
