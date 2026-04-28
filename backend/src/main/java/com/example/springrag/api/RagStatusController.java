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
        return new RagStatusResponse(
                ragProperties.getEmbedding().getMode(),
                ragProperties.getStore().getMode(),
                new ChromaStatus(
                        ragProperties.getStore().getChroma().getBaseUrl(),
                        ragProperties.getStore().getChroma().getApiVersion(),
                        ragProperties.getStore().getChroma().getTenant(),
                        ragProperties.getStore().getChroma().getDatabase(),
                        ragProperties.getStore().getChroma().getCollection()
                ),
                new RetrievalStatus(
                        ragProperties.getRetrieval().getTopK(),
                        ragProperties.getRetrieval().getMinScore(),
                        ragProperties.getRetrieval().getMaxRetries()
                ),
                new LlmStatus(
                        ragProperties.getLlm().isEnabled(),
                        ragProperties.getLlm().getProvider(),
                        ragProperties.getLlm().getModel(),
                        ragProperties.getLlm().getBaseUrl()
                )
        );
    }

    public record RagStatusResponse(
            String embeddingMode,
            String storeMode,
            ChromaStatus chroma,
            RetrievalStatus retrieval,
            LlmStatus llm
    ) {
    }

    public record ChromaStatus(String baseUrl, String apiVersion, String tenant, String database, String collection) {
    }

    public record RetrievalStatus(int topK, double minScore, int maxRetries) {
    }

    public record LlmStatus(boolean enabled, String provider, String model, String baseUrl) {
    }
}
