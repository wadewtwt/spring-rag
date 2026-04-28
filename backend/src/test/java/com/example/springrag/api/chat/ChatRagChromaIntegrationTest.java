package com.example.springrag.api.chat;

import com.example.springrag.application.chat.RagLlmGateway;
import com.example.springrag.application.chat.RetrievalEvaluation;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.rag.llm.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("chroma")
class ChatRagChromaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @MockBean
    private RagLlmGateway ragLlmGateway;

    @Test
    @EnabledIf("isChromaAvailable")
    void shouldUseChromaStoreWhenProfileIsActive() throws Exception {
        when(ragLlmGateway.evaluateRetrieval(anyString(), anyString(), anyList(), anyList()))
                .thenReturn(new RetrievalEvaluation(true, "enough context"));
        when(ragLlmGateway.generateAnswer(anyString(), anyString(), anyList(), anyList()))
                .thenReturn("The warranty period is two years according to guide.md.");

        assertThat(embeddingStore).isInstanceOf(ChromaEmbeddingStore.class);

        mockMvc.perform(get("/api/rag/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeMode").value("chroma"));

        mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile(
                                "file",
                                "guide.md",
                                MediaType.TEXT_PLAIN_VALUE,
                                """
                                Warranty period is two years.
                                Contact online support before repair.
                                """.getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"threadId":"thread-chroma","message":"warranty period"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Warranty period is two years")))
                .andExpect(content().string(containsString("guide.md")));
    }

    static boolean isChromaAvailable() {
        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(1))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:8000/api/v1/heartbeat"))
                    .timeout(Duration.ofSeconds(1))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() < 400;
        } catch (IOException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
