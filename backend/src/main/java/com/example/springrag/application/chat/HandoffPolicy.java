package com.example.springrag.application.chat;

/**
 * 人工接管策略接口。
 * <p>
 * 当系统发现当前对话可能无法很好处理时，可以建议“转人工”。
 * 这里把判断逻辑抽象成接口，是为了把“是否转人工”的规则
 * 从具体聊天流程中拆出来，后续可以独立替换实现。
 */
public interface HandoffPolicy {

    /**
     * 判断当前消息是否应该建议转人工。
     *
     * @param message 用户当前发送的消息
     * @param consecutiveFailures 当前会话累计的连续失败次数
     * @return 如果应该建议转人工，则返回 {@code true}
     */
    boolean shouldHandoff(String message, int consecutiveFailures);
}
