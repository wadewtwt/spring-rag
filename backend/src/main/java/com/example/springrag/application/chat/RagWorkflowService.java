package com.example.springrag.application.chat;

import com.example.springrag.application.document.KnowledgeBaseService;
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

    public RagWorkflowResult run(String question, List<SourceReference> sources) {
        if (sources.isEmpty()) {
            return new RagWorkflowResult(
                    "已收到你的问题：" + question + "。当前知识库里还没有足够匹配的资料，请补充文档后再试。",
                    false,
                    question,
                    0,
                    List.of()
            );
        }
        SourceReference first = sources.get(0);
        return new RagWorkflowResult(
                "根据知识库《" + first.title() + "》的内容，" + first.snippet(),
                true,
                question,
                0,
                sources
        );
    }

    public RagWorkflowResult run(String question) throws Exception {
        if (knowledgeBaseService == null || ragLlmGateway == null) {
            throw new IllegalStateException("RagWorkflowService requires knowledgeBaseService and ragLlmGateway");
        }

        var finalState = buildGraph().invoke(Map.of(
                "question", question,
                "currentQuery", question,
                "sources", List.of(),
                "retryCount", 0
        )).orElseThrow();

        return new RagWorkflowResult(
                finalState.answer().orElse(""),
                finalState.retrievalSatisfied().orElse(false),
                finalState.currentQuery().orElse(question),
                finalState.retryCount().orElse(0),
                finalState.sources()
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
                            state.sources()
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
                            state.sources()
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
                                state.sources()
                        )
                )))
                .addNode("fallback", node_async(state -> Map.of(
                        "answer", "当前知识库里还没有足够匹配的资料来回答这个问题，请补充相关文档后再试。"
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
