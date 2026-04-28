package com.example.springrag.infrastructure.chat;

import com.example.springrag.application.chat.RagLlmGateway;
import com.example.springrag.application.chat.RetrievalEvaluation;
import com.example.springrag.config.RagProperties;
import com.example.springrag.domain.chat.SourceReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DashScopeRagLlmGateway implements RagLlmGateway {

    private final RagProperties.Llm properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DashScopeRagLlmGateway(RagProperties.Llm properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    @Override
    public String rewriteQuestion(String question, String currentQuery, int retryCount, List<SourceReference> sources) {
        String prompt = """
                Rewrite the retrieval query for a RAG search.
                Keep the user intent unchanged.
                Return only the rewritten query.

                User question: %s
                Current query: %s
                Retry count: %d
                """.formatted(question, currentQuery, retryCount);
        return chat(prompt);
    }

    @Override
    public RetrievalEvaluation evaluateRetrieval(String question, String currentQuery, List<SourceReference> sources) {
        String prompt = """
                Decide whether the retrieved snippets are sufficient to answer the user question.
                Return JSON only, using this shape:
                {"satisfied":true|false,"reason":"short reason"}

                User question: %s
                Current query: %s
                Sources:
                %s
                """.formatted(question, currentQuery, renderSources(sources));
        String content = chat(prompt);
        try {
            JsonNode node = objectMapper.readTree(content);
            return new RetrievalEvaluation(
                    node.path("satisfied").asBoolean(false),
                    node.path("reason").asText("")
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to parse retrieval evaluation response", exception);
        }
    }

    @Override
    public String generateAnswer(String question, String currentQuery, List<SourceReference> sources) {
        String prompt = """
                Answer the user question using only the provided knowledge base snippets.
                If the snippets are insufficient, say so explicitly.

                User question: %s
                Current query: %s
                Sources:
                %s
                """.formatted(question, currentQuery, renderSources(sources));
        return chat(prompt);
    }

    private String chat(String prompt) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", properties.getModel(),
                    "temperature", properties.getTemperature(),
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", prompt
                    ))
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getBaseUrl() + "/chat/completions"))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("DashScope request failed with status " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            return root.path("choices").path(0).path("message").path("content").asText();
        } catch (IOException | InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("DashScope request failed", exception);
        }
    }

    private String renderSources(List<SourceReference> sources) {
        return sources.stream()
                .map(source -> source.title() + ": " + source.snippet())
                .collect(Collectors.joining("\n"));
    }
}
