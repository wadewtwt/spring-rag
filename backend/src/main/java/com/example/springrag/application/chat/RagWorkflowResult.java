package com.example.springrag.application.chat;

/**
 * RAG 工作流执行结果。
 * <p>
 * 这是一个很典型的“结果对象”：
 * 工作流内部可能经历多个节点，但对外部调用方来说，
 * 最关心的通常只是最终答案，以及这次检索是否满足要求。
 *
 * @param answer 最终生成给用户的答案
 * @param retrievalSatisfied 检索结果是否满足当前回答需要
 */
public record RagWorkflowResult(String answer, boolean retrievalSatisfied) {
}
