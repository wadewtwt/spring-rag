package com.example.springrag.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RagStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeCurrentRagModes() throws Exception {
        mockMvc.perform(get("/api/rag/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.embeddingMode").value("simple"))
                .andExpect(jsonPath("$.storeMode").value("inmemory"))
                .andExpect(jsonPath("$.chroma.baseUrl").value("http://localhost:8000"));
    }
}
