package com.example.springrag.api.chat;

import com.example.springrag.application.chat.RagLlmGateway;
import com.example.springrag.application.chat.RetrievalEvaluation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RagLlmGateway ragLlmGateway;

    @Test
    void shouldReturnServerSentEvents() throws Exception {
        when(ragLlmGateway.evaluateRetrieval(anyString(), anyString(), anyList()))
                .thenReturn(new RetrievalEvaluation(false, "no context"));
        when(ragLlmGateway.rewriteQuestion(anyString(), anyString(), anyInt(), anyList()))
                .thenReturn("你好");

        mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"threadId":"thread-1","message":"你好"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/event-stream")));
    }
}
