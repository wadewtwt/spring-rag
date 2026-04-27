package com.example.springrag.infrastructure.chat;

import com.example.springrag.application.chat.HandoffPolicy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于关键字和失败次数的人工接管策略。
 * <p>
 * 这是一种非常朴素的实现：
 * 1. 如果用户消息里出现明显负向或人工诉求关键词，就建议转人工；
 * 2. 如果连续失败次数过多，也建议转人工。
 * <p>
 * 它的优点是简单直接、非常容易理解；
 * 缺点是规则较粗糙，真实场景下可能需要更复杂的情绪识别和策略判断。
 */
@Component
public class KeywordHandoffPolicy implements HandoffPolicy {

    /**
     * 一组触发人工接管的示例关键词。
     */
    private static final List<String> NEGATIVE_KEYWORDS = List.of("投诉", "差评", "人工", "生气", "没解决");

    /**
     * 判断是否建议转人工。
     *
     * @param message 用户当前发送的消息
     * @param consecutiveFailures 连续失败次数
     * @return 命中关键词或失败次数过多时返回 {@code true}
     */
    @Override
    public boolean shouldHandoff(String message, int consecutiveFailures) {
        return consecutiveFailures >= 2 || NEGATIVE_KEYWORDS.stream().anyMatch(message::contains);
    }
}
