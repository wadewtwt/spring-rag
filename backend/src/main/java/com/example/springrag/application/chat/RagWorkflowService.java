package com.example.springrag.application.chat;

import com.example.springrag.domain.chat.SourceReference;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;

import java.util.List;
import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

public class RagWorkflowService {

    public RagWorkflowResult run(String question, List<SourceReference> sources) throws Exception {
        var workflow = buildGraph();
        var finalState = workflow.invoke(Map.of(
                "question", question,
                "sources", sources
        )).orElseThrow();

        return new RagWorkflowResult(
                finalState.answer().orElse(""),
                finalState.retrievalSatisfied().orElse(false)
        );
    }

    private org.bsc.langgraph4j.CompiledGraph<RagWorkflowState> buildGraph() throws Exception {
        AsyncEdgeAction<RagWorkflowState> routeAfterEvaluate = edge_async(state ->
                state.retrievalSatisfied().orElse(false) ? "generator" : "fallback");

        return new StateGraph<>(RagWorkflowState::new)
                .addNode("rewriter", node_async(state -> {
                    // 一期先保留为最小改写节点，后续这里可以接查询重写模型。
                    return Map.of("question", state.question().orElse(""));
                }))
                .addNode("evaluator", node_async(state -> Map.of(
                        "retrievalSatisfied", !state.sources().isEmpty()
                )))
                .addNode("generator", node_async(state -> {
                    SourceReference first = state.sources().get(0);
                    return Map.of("answer",
                            "根据知识库《" + first.title() + "》的内容：" + first.snippet());
                }))
                .addNode("fallback", node_async(state -> Map.of(
                        "answer", "已收到你的问题：" + state.question().orElse("") + "。当前知识库里还没有足够匹配的资料，请补充文档后再试。"
                )))
                .addEdge(START, "rewriter")
                .addEdge("rewriter", "evaluator")
                .addConditionalEdges("evaluator", routeAfterEvaluate, Map.of(
                        "generator", "generator",
                        "fallback", "fallback"
                ))
                .addEdge("generator", END)
                .addEdge("fallback", END)
                .compile();
    }
}
