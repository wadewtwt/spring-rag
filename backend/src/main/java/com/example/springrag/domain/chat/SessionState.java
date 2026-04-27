package com.example.springrag.domain.chat;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次会话的领域状态对象。
 * <p>
 * 可以把它理解成“线程 ID 对应的聊天上下文快照”。
 * 当前版本主要保存：
 * 1. 历史消息
 * 2. 上一次检索到的来源
 * 3. 是否建议转人工
 * 4. 连续失败次数
 * <p>
 * 这个类位于 domain 层，说明它表达的是“业务数据结构”，
 * 而不是某个具体技术框架下的实现细节。
 */
public class SessionState {

    /**
     * 会话线程 ID，用来标识一条独立对话。
     */
    private final String threadId;

    /**
     * 当前会话里的消息历史。
     */
    private final List<ChatMessage> messages;

    /**
     * 最近一次回答时所参考的来源片段。
     */
    private List<SourceReference> lastSources;

    /**
     * 当前会话是否建议转人工处理。
     */
    private boolean handoffSuggested;

    /**
     * 连续失败次数。
     * 这个值通常可以用来驱动降级策略或人工接管策略。
     */
    private int consecutiveFailures;

    private SessionState(String threadId, List<ChatMessage> messages) {
        this.threadId = threadId;
        this.messages = messages;
        this.lastSources = new ArrayList<>();
    }

    /**
     * 创建一个空会话。
     *
     * @param threadId 会话线程 ID
     * @return 初始为空的新会话状态
     */
    public static SessionState empty(String threadId) {
        return new SessionState(threadId, new ArrayList<>());
    }

    /**
     * 向消息历史中追加一条消息。
     *
     * @param message 待追加的消息
     */
    public void appendMessage(ChatMessage message) {
        this.messages.add(message);
    }

    /**
     * @return 当前会话的线程 ID
     */
    public String getThreadId() {
        return threadId;
    }

    /**
     * @return 会话中的完整消息历史
     */
    public List<ChatMessage> getMessages() {
        return messages;
    }

    /**
     * @return 上次回答使用到的来源片段
     */
    public List<SourceReference> getLastSources() {
        return lastSources;
    }

    /**
     * 更新上次回答的来源列表。
     * <p>
     * 这里会复制一份新列表，而不是直接保存传入对象，
     * 这样可以降低外部继续修改原列表带来的副作用。
     *
     * @param lastSources 最新来源列表
     */
    public void setLastSources(List<SourceReference> lastSources) {
        this.lastSources = new ArrayList<>(lastSources);
    }

    /**
     * @return 当前是否建议人工接管
     */
    public boolean isHandoffSuggested() {
        return handoffSuggested;
    }

    /**
     * 设置是否建议人工接管。
     *
     * @param handoffSuggested 是否建议人工接管
     */
    public void setHandoffSuggested(boolean handoffSuggested) {
        this.handoffSuggested = handoffSuggested;
    }

    /**
     * @return 当前连续失败次数
     */
    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    /**
     * 连续失败次数加一。
     * <p>
     * 一般在回答失败、检索不足或命中异常场景时调用。
     */
    public void incrementFailures() {
        this.consecutiveFailures++;
    }

    /**
     * 连续失败次数清零。
     * <p>
     * 一般在一次成功处理后调用。
     */
    public void resetFailures() {
        this.consecutiveFailures = 0;
    }
}
