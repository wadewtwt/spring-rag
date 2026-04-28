package com.example.springrag.application.chat;

import com.example.springrag.application.chat.support.FakeWorkflowKnowledgeBaseService;
import com.example.springrag.application.chat.support.FakeWorkflowLlmGateway;
import com.example.springrag.domain.chat.ChatMessage;
import com.example.springrag.domain.chat.SourceReference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagWorkflowServiceTest {

    @Test
    void shouldGenerateAnswerWhenInitialRetrievalIsSufficient() throws Exception {
        FakeWorkflowKnowledgeBaseService knowledgeBaseService = new FakeWorkflowKnowledgeBaseService()
                .whenQuery("What is the warranty period?", List.of(
                        new SourceReference("guide.md", "Warranty period is two years.")
                ));
        FakeWorkflowLlmGateway llmGateway = new FakeWorkflowLlmGateway()
                .withEvaluation("What is the warranty period?", true, "enough context")
                .withAnswer("What is the warranty period?",
                        "The warranty period is two years according to guide.md.");
        RagWorkflowService workflowService = new RagWorkflowService(knowledgeBaseService, llmGateway, 1);

        RagWorkflowResult result = workflowService.run("What is the warranty period?");

        assertThat(result.answer()).contains("two years");
        assertThat(result.retrievalSatisfied()).isTrue();
        assertThat(result.finalQuery()).isEqualTo("What is the warranty period?");
        assertThat(result.retryCount()).isZero();
    }

    @Test
    void shouldRewriteAndRetryWhenInitialRetrievalIsInsufficient() throws Exception {
        FakeWorkflowKnowledgeBaseService knowledgeBaseService = new FakeWorkflowKnowledgeBaseService()
                .whenQuery("refund?", List.of())
                .whenQuery("refund policy", List.of(
                        new SourceReference("policy.md", "Refunds are allowed within seven days.")
                ));
        FakeWorkflowLlmGateway llmGateway = new FakeWorkflowLlmGateway()
                .withRewrite("refund?", "refund policy")
                .withEvaluation("refund?", false, "too vague")
                .withEvaluation("refund policy", true, "matched policy context")
                .withAnswer("refund policy",
                        "Refunds are allowed within seven days according to policy.md.");
        RagWorkflowService workflowService = new RagWorkflowService(knowledgeBaseService, llmGateway, 1);

        RagWorkflowResult result = workflowService.run("refund?");

        assertThat(result.retrievalSatisfied()).isTrue();
        assertThat(result.answer()).contains("seven days");
        assertThat(result.finalQuery()).isEqualTo("refund policy");
        assertThat(result.retryCount()).isEqualTo(1);
    }

    @Test
    void shouldFallbackWhenRetriesAreExhausted() throws Exception {
        FakeWorkflowKnowledgeBaseService knowledgeBaseService = new FakeWorkflowKnowledgeBaseService()
                .whenQuery("unknown question", List.of())
                .whenQuery("expanded unknown question", List.of());
        FakeWorkflowLlmGateway llmGateway = new FakeWorkflowLlmGateway()
                .withRewrite("unknown question", "expanded unknown question")
                .withEvaluation("unknown question", false, "no evidence")
                .withEvaluation("expanded unknown question", false, "still no evidence");
        RagWorkflowService workflowService = new RagWorkflowService(knowledgeBaseService, llmGateway, 1);

        RagWorkflowResult result = workflowService.run("unknown question");

        assertThat(result.answer()).contains("knowledge base");
        assertThat(result.retrievalSatisfied()).isFalse();
        assertThat(result.finalQuery()).isEqualTo("expanded unknown question");
        assertThat(result.retryCount()).isEqualTo(1);
    }

    @Test
    void shouldPassRecentConversationHistoryToAnswerGeneration() throws Exception {
        FakeWorkflowKnowledgeBaseService knowledgeBaseService = new FakeWorkflowKnowledgeBaseService()
                .whenQuery("When does it expire?", List.of(
                        new SourceReference("guide.md", "The warranty expires after two years.")
                ));
        FakeWorkflowLlmGateway llmGateway = new FakeWorkflowLlmGateway()
                .withEvaluation("When does it expire?", true, "enough context")
                .withAnswer("When does it expire?", "It expires after two years.");
        RagWorkflowService workflowService = new RagWorkflowService(knowledgeBaseService, llmGateway, 1);

        workflowService.run(
                "When does it expire?",
                List.of(
                        new ChatMessage("user", "What is the warranty period?"),
                        new ChatMessage("assistant", "The warranty period is two years according to guide.md.")
                )
        );

        assertThat(llmGateway.lastAnswerHistory())
                .extracting(ChatMessage::content)
                .containsExactly(
                        "What is the warranty period?",
                        "The warranty period is two years according to guide.md."
                );
    }
}
