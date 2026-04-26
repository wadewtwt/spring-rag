package com.example.springrag.api;

import com.example.springrag.config.RagProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class RagStatusController {

    private final RagProperties ragProperties;

    public RagStatusController(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    @GetMapping("/status")
    public RagStatusResponse status() {
        // 这个接口主要用于联调时快速确认当前到底走的是哪套 embedding 和 store 配置。
        return new RagStatusResponse(
                ragProperties.getEmbedding().getMode(),
                ragProperties.getStore().getMode(),
                new ChromaStatus(
                        ragProperties.getStore().getChroma().getBaseUrl(),
                        ragProperties.getStore().getChroma().getTenant(),
                        ragProperties.getStore().getChroma().getDatabase(),
                        ragProperties.getStore().getChroma().getCollection()
                )
        );
    }

    public record RagStatusResponse(String embeddingMode, String storeMode, ChromaStatus chroma) {
    }

    public record ChromaStatus(String baseUrl, String tenant, String database, String collection) {
    }
}
