package com.example.springrag.api.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatRagIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldUseUploadedKnowledgeWhenAnsweringQuestion() throws Exception {
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
    }
}
