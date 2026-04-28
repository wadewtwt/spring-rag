package com.example.springrag.application.chat;

import com.example.springrag.application.document.KnowledgeBaseService;
import com.example.springrag.domain.chat.ChatMessage;
import com.example.springrag.domain.chat.SourceReference;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;

import java.util.List;
import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

public class RagWorkflowService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final RagLlmGateway ragLlmGateway;
    private final int maxRetries;

    public RagWorkflowService() {
        this.knowledgeBaseService = null;
        this.ragLlmGateway = null;
        this.maxRetries = 0;
    }

    public RagWorkflowService(KnowledgeBaseService knowledgeBaseService,
                              RagLlmGateway ragLlmGateway,
                              int maxRetries) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.ragLlmGateway = ragLlmGateway;
        this.maxRetries = Math.max(0, maxRetries);
    }

    public RagWorkflowResult run(String question) throws Exception {
        return run(question, List.of());
    }

    public RagWorkflowResult runWithoutLlm(String question) {
        if (knowledgeBaseService == null) {
            throw new IllegalStateException("RagWorkflowService requires knowledgeBaseService");
        }

        return answerFromRetrievedSources(question, knowledgeBaseService.search(question));
    }

    public RagWorkflowResult run(String question, List<ChatMessage> history) throws Exception {
        if (knowledgeBaseService == null || ragLlmGateway == null) {
            throw new IllegalStateException("RagWorkflowService requires knowledgeBaseService and ragLlmGateway");
        }

        var finalState = buildGraph().invoke(Map.of(
                "question", question,
                "currentQuery", question,
                "sources", List.of(),
                "retryCount", 0,
                "history", List.copyOf(history)
        )).orElseThrow();

        return new RagWorkflowResult(
                finalState.answer().orElse(""),
                finalState.retrievalSatisfied().orElse(false),
                finalState.currentQuery().orElse(question),
                finalState.retryCount().orElse(0),
                finalState.sources()
        );
    }

    private RagWorkflowResult answerFromRetrievedSources(String question, List<SourceReference> sources) {
        if (sources.isEmpty()) {
            return new RagWorkflowResult(
                    "The current knowledge base does not contain enough matching information to answer this question.",
                    false,
                    question,
                    0,
                    List.of()
            );
        }

        SourceReference first = sources.get(0);
        return new RagWorkflowResult(
                "According to " + first.title() + ", " + first.snippet(),
                true,
                question,
                0,
                sources
        );
    }

    private org.bsc.langgraph4j.CompiledGraph<RagWorkflowState> buildGraph() throws Exception {
        AsyncEdgeAction<RagWorkflowState> routeAfterEvaluate = edge_async(state -> {
            if (state.retrievalSatisfied().orElse(false)) {
                return "generator";
            }
            int retryCount = state.retryCount().orElse(0);
            return retryCount < maxRetries ? "rewriter" : "fallback";
        });

        return new StateGraph<>(RagWorkflowState::new)
                .addNode("retriever", node_async(state -> Map.of(
                        "sources", knowledgeBaseService.search(state.currentQuery().orElse(state.question().orElse("")))
                )))
                .addNode("evaluator", node_async(state -> {
                    if (state.sources().isEmpty()) {
                        return Map.of(
                                "retrievalSatisfied", false,
                                "evaluationReason", "No retrieved sources"
                        );
                    }

                    RetrievalEvaluation evaluation = ragLlmGateway.evaluateRetrieval(
                            state.question().orElse(""),
                            state.currentQuery().orElse(""),
                            state.sources(),
                            state.history()
                    );
                    return Map.of(
                            "retrievalSatisfied", evaluation.satisfied(),
                            "evaluationReason", evaluation.reason()
                    );
                }))
                .addNode("rewriter", node_async(state -> {
                    int nextRetryCount = state.retryCount().orElse(0) + 1;
                    String rewrittenQuery = ragLlmGateway.rewriteQuestion(
                            state.question().orElse(""),
                            state.currentQuery().orElse(""),
                            state.retryCount().orElse(0),
                            state.sources(),
                            state.history()
                    );
                    return Map.of(
                            "currentQuery", rewrittenQuery,
                            "retryCount", nextRetryCount
                    );
                }))
                .addNode("generator", node_async(state -> Map.of(
                        "answer", ragLlmGateway.generateAnswer(
                                state.question().orElse(""),
                                state.currentQuery().orElse(""),
                                state.sources(),
                                state.history()
                        )
                )))
                .addNode("fallback", node_async(state -> Map.of(
                        "answer", "The current knowledge base does not contain enough matching information to answer this question."
                )))
                .addEdge(START, "retriever")
                .addEdge("retriever", "evaluator")
                .addConditionalEdges("evaluator", routeAfterEvaluate, Map.of(
                        "generator", "generator",
                        "rewriter", "rewriter",
                        "fallback", "fallback"
                ))
                .addEdge("rewriter", "retriever")
                .addEdge("generator", END)
                .addEdge("fallback", END)
                .compile();
    }
}
