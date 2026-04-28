package com.example.springrag.api.chat;

import com.example.springrag.application.chat.RagLlmGateway;
import com.example.springrag.application.chat.RetrievalEvaluation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.rag.llm.enabled=true")
@AutoConfigureMockMvc
class ChatRagIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RagLlmGateway ragLlmGateway;

    @Test
    void shouldUseUploadedKnowledgeWhenAnsweringQuestion() throws Exception {
        when(ragLlmGateway.evaluateRetrieval(anyString(), anyString(), anyList(), anyList()))
                .thenReturn(new RetrievalEvaluation(true, "enough context"));
        when(ragLlmGateway.generateAnswer(anyString(), anyString(), anyList(), anyList()))
                .thenReturn("The warranty period is two years according to guide.md.");

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
                                {"threadId":"thread-rag","message":"warranty period"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Warranty period is two years")))
                .andExpect(content().string(containsString("guide.md")));

        verify(ragLlmGateway).evaluateRetrieval(anyString(), anyString(), anyList(), anyList());
        verify(ragLlmGateway).generateAnswer(anyString(), anyString(), anyList(), anyList());
    }

    @Test
    void shouldReuseConversationHistoryForFollowUpQuestion() throws Exception {
        when(ragLlmGateway.evaluateRetrieval(anyString(), anyString(), anyList(), anyList()))
                .thenReturn(new RetrievalEvaluation(true, "enough context"));
        when(ragLlmGateway.generateAnswer(eq("What is the warranty period?"), anyString(), anyList(), anyList()))
                .thenReturn("The warranty period is two years.");
        when(ragLlmGateway.generateAnswer(eq("When does it expire?"), anyString(), anyList(), anyList()))
                .thenReturn("It expires after two years.");

        mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"threadId":"thread-follow-up","message":"What is the warranty period?"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"threadId":"thread-follow-up","message":"When does it expire?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("It expires after two years.")));

        verify(ragLlmGateway, atLeastOnce()).generateAnswer(
                eq("When does it expire?"),
                anyString(),
                anyList(),
                argThat(history -> history.size() >= 2
                        && history.get(0).content().contains("What is the warranty period?")
                        && history.get(1).content().contains("The warranty period is two years."))
        );
    }
}
