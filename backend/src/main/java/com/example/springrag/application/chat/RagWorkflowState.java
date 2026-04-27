package com.example.springrag.application.chat;

import com.example.springrag.domain.chat.SourceReference;
import org.bsc.langgraph4j.state.AgentState;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * RAG 工作流在状态机中流转的共享状态对象。
 * <p>
 * 这里继承了 LangGraph4j 的 {@link AgentState}，
 * 本质上可以把它理解成一个“节点之间共享的上下文容器”。
 * 每个节点都可以从这里读取已有数据，也可以往里面写入新数据。
 * <p>
 * 当前这份状态里主要保存：
 * 问题、检索到的来源、检索是否满足要求、最终答案。
 */
public class RagWorkflowState extends AgentState {

    /**
     * 创建一个空状态。
     * <p>
     * 状态机会在启动时基于这个空壳子逐步填充数据。
     */
    public RagWorkflowState() {
        super(Map.of());
    }

    /**
     * 使用已有数据初始化状态。
     *
     * @param initData 初始状态数据
     */
    public RagWorkflowState(Map<String, Object> initData) {
        super(initData);
    }

    /**
     * 读取当前问题。
     *
     * @return 如果状态中存在 question，就返回对应值
     */
    public Optional<String> question() {
        return value("question");
    }

    /**
     * 读取当前检索到的来源列表。
     * <p>
     * 这里使用了类型转换，是因为底层状态容器按 Object 存储值，
     * 取出来时需要手动告诉 Java 真实类型是什么。
     *
     * @return 来源引用列表；如果没有值，返回空列表
     */
    @SuppressWarnings("unchecked")
    public List<SourceReference> sources() {
        return (List<SourceReference>) value("sources").orElse(List.of());
    }

    /**
     * 读取“检索是否满足要求”的判断结果。
     *
     * @return 检索满意度标记
     */
    public Optional<Boolean> retrievalSatisfied() {
        return value("retrievalSatisfied");
    }

    /**
     * 读取最终答案。
     *
     * @return 如果工作流已经产出答案，就返回该答案
     */
    public Optional<String> answer() {
        return value("answer");
    }
}
