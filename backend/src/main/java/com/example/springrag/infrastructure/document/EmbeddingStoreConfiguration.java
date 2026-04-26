package com.example.springrag.infrastructure.document;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaApiVersion;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingStoreConfiguration {

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(
            @Value("${app.rag.store.mode:inmemory}") String mode,
            @Value("${app.rag.store.chroma.base-url:http://localhost:8000}") String baseUrl,
            @Value("${app.rag.store.chroma.tenant:default_tenant}") String tenant,
            @Value("${app.rag.store.chroma.database:default_database}") String database,
            @Value("${app.rag.store.chroma.collection:spring-rag}") String collection,
            EmbeddingStoreFactory factory) {
        if ("chroma".equalsIgnoreCase(mode)) {
            return factory.createChromaOrFallback(() -> ChromaEmbeddingStore.builder()
                    .apiVersion(ChromaApiVersion.V2)
                    .baseUrl(baseUrl)
                    .tenantName(tenant)
                    .databaseName(database)
                    .collectionName(collection)
                    .build());
        }
        return new InMemoryEmbeddingStore<>();
    }
}
