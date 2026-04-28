package com.example.springrag.infrastructure.chat;

import com.example.springrag.application.chat.RetrievalEvaluation;
import com.example.springrag.config.RagProperties;
import com.example.springrag.domain.chat.SourceReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DashScopeRagLlmGatewayTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldGenerateAnswerFromDashScopeCompatibleResponse() throws Exception {
        String body = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "The warranty period is two years."
                      }
                    }
                  ]
                }
                """;
        server = startServer(body);

        DashScopeRagLlmGateway gateway = new DashScopeRagLlmGateway(properties(serverUrl()), new ObjectMapper());

        String answer = gateway.generateAnswer(
                "What is the warranty period?",
                "What is the warranty period?",
                List.of(new SourceReference("guide.md", "Warranty period is two years."))
        );

        assertThat(answer).isEqualTo("The warranty period is two years.");
    }

    @Test
    void shouldParseStructuredRetrievalEvaluation() throws Exception {
        String body = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\\"satisfied\\\":true,\\\"reason\\\":\\\"The snippet answers the question.\\\"}"
                      }
                    }
                  ]
                }
                """;
        server = startServer(body);

        DashScopeRagLlmGateway gateway = new DashScopeRagLlmGateway(properties(serverUrl()), new ObjectMapper());

        RetrievalEvaluation evaluation = gateway.evaluateRetrieval(
                "What is the warranty period?",
                "What is the warranty period?",
                List.of(new SourceReference("guide.md", "Warranty period is two years."))
        );

        assertThat(evaluation.satisfied()).isTrue();
        assertThat(evaluation.reason()).contains("answers the question");
    }

    @Test
    void shouldRejectCallsWhenGatewayIsDisabled() {
        DisabledRagLlmGateway gateway = new DisabledRagLlmGateway();

        assertThatThrownBy(() -> gateway.generateAnswer("q", "q", List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");
    }

    private HttpServer startServer(String responseBody) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/chat/completions", exchange -> writeJson(exchange, responseBody));
        httpServer.start();
        return httpServer;
    }

    private void writeJson(HttpExchange exchange, String responseBody) throws IOException {
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private String serverUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private RagProperties.Llm properties(String baseUrl) {
        RagProperties.Llm llm = new RagProperties.Llm();
        llm.setEnabled(true);
        llm.setApiKey("test-key");
        llm.setBaseUrl(baseUrl);
        llm.setModel("qwen-plus");
        llm.setTemperature(0.2);
        llm.setTimeoutSeconds(5);
        return llm;
    }
}
