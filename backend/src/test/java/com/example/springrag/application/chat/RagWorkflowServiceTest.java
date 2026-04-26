package com.example.springrag.application.chat;

import com.example.springrag.domain.chat.SourceReference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagWorkflowServiceTest {

    @Test
    void shouldUseGeneratorBranchWhenSourcesExist() throws Exception {
        RagWorkflowService workflowService = new RagWorkflowService();

        RagWorkflowResult result = workflowService.run("What is the warranty period?", List.of(
                new SourceReference("guide.md", "Warranty period is two years.")
        ));

        assertThat(result.answer()).contains("guide.md");
        assertThat(result.answer()).contains("Warranty period is two years");
        assertThat(result.retrievalSatisfied()).isTrue();
    }

    @Test
    void shouldUseFallbackBranchWhenSourcesAreEmpty() throws Exception {
        RagWorkflowService workflowService = new RagWorkflowService();

        RagWorkflowResult result = workflowService.run("unknown question", List.of());

        assertThat(result.answer()).contains("没有足够匹配的资料");
        assertThat(result.retrievalSatisfied()).isFalse();
    }
}
