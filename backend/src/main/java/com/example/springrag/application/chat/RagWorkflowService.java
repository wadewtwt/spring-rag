package com.example.springrag.application.chat;

import com.example.springrag.domain.chat.SourceReference;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;

import java.util.List;
import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * RAG 工作流编排服务。
 * <p>
 * 这个类是当前项目里最接近“RAG 流程引擎”的地方。
 * 它不直接暴露 HTTP 接口，而是在应用层把一个问题如何经过
 * “改写 -> 评估 -> 生成 / 回退” 组织成一张状态图。
 * <p>
 * 这里使用 LangGraph4j 的意义在于：
 * 用状态机的方式把流程节点和分支显式表达出来，
 * 方便后续把简单占位逻辑逐步替换成真正的 LLM、评估器、重试逻辑。
 */
public class RagWorkflowService {

    /**
     * 执行一次 RAG 工作流。
     *
     * @param question 用户问题
     * @param sources 检索阶段召回的来源片段
     * @return 工作流的最终结果
     * @throws Exception 构图或执行状态图失败时抛出
     */
    public RagWorkflowResult run(String question, List<SourceReference> sources) throws Exception {
        var workflow = buildGraph();

        // 启动状态机时，把最初的输入问题和检索来源放进共享状态中。
        var finalState = workflow.invoke(Map.of(
                "question", question,
                "sources", sources
        )).orElseThrow();

        // 从最终状态里取出对外真正关心的两个结果：答案和检索是否满足要求。
        return new RagWorkflowResult(
                finalState.answer().orElse(""),
                finalState.retrievalSatisfied().orElse(false)
        );
    }

    /**
     * 构建一张最小可运行的 RAG 状态图。
     * <p>
     * 当前图中的节点含义如下：
     * 1. rewriter：问题改写节点
     * 2. evaluator：检索结果评估节点
     * 3. generator：有可用来源时生成答案
     * 4. fallback：没有可用来源时给出回退提示
     *
     * @return 编译后的状态图
     * @throws Exception 状态图编译失败时抛出
     */
    private org.bsc.langgraph4j.CompiledGraph<RagWorkflowState> buildGraph() throws Exception {
        // 根据 evaluator 节点写入的 retrievalSatisfied 决定后续分支。
        AsyncEdgeAction<RagWorkflowState> routeAfterEvaluate = edge_async(state ->
                state.retrievalSatisfied().orElse(false) ? "generator" : "fallback");

        return new StateGraph<>(RagWorkflowState::new)
                .addNode("rewriter", node_async(state -> {
                    // 当前版本先把原问题原样透传。
                    // 后续这里可以接入“查询改写”模型，把口语化问题改得更适合检索。
                    return Map.of("question", state.question().orElse(""));
                }))
                .addNode("evaluator", node_async(state -> {
                    // 当前版本的评估逻辑非常简单：
                    // 只要检索来源不为空，就认为这次检索“基本可用”。
                    // 真正成熟的实现一般会进一步看分数、覆盖度、相关性等指标。
                    return Map.of("retrievalSatisfied", !state.sources().isEmpty());
                }))
                .addNode("generator", node_async(state -> {
                    // 为了让流程先跑通，当前只拿第一条来源拼一个演示答案。
                    // 后续这里最适合替换成真正的大模型生成节点。
                    SourceReference first = state.sources().get(0);
                    return Map.of("answer",
                            "根据知识库《" + first.title() + "》的内容，" + first.snippet());
                }))
                .addNode("fallback", node_async(state -> Map.of(
                        // 当检索不到足够资料时，先给用户一个明确、可解释的回退答案。
                        "answer", "已收到你的问题：" + state.question().orElse("")
                                + "。当前知识库里还没有足够匹配的资料，请补充文档后再试。"
                )))
                // 定义整张图的流转顺序。
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
