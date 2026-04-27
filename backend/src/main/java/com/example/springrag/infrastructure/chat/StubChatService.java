package com.example.springrag.infrastructure.chat;

import com.example.springrag.api.chat.ChatStreamRequest;
import com.example.springrag.application.chat.ChatService;
import com.example.springrag.application.chat.HandoffPolicy;
import com.example.springrag.application.chat.RagWorkflowResult;
import com.example.springrag.application.chat.RagWorkflowService;
import com.example.springrag.application.chat.SessionStateRepository;
import com.example.springrag.application.document.KnowledgeBaseService;
import com.example.springrag.domain.chat.ChatMessage;
import com.example.springrag.domain.chat.SessionState;
import com.example.springrag.domain.chat.SourceReference;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

/**
 * 当前项目里的一个“占位型”聊天服务实现。
 * <p>
 * 它已经把一条完整聊天链路串起来了：
 * 1. 读取会话状态
 * 2. 记录用户消息
 * 3. 检索知识库
 * 4. 执行 RAG 工作流
 * 5. 判断是否建议转人工
 * 6. 保存会话状态
 * 7. 通过 SSE 把结果推给前端
 * <p>
 * 名字里带 {@code Stub}，是因为它还不是完整生产实现，
 * 但非常适合作为学习入口，因为链路已经很清晰。
 */
@Service
public class StubChatService implements ChatService {

    /**
     * 用来加载和保存会话状态。
     */
    private final SessionStateRepository sessionStateRepository;

    /**
     * 用来判断是否需要建议转人工。
     */
    private final HandoffPolicy handoffPolicy;

    /**
     * 用来检索知识库来源。
     */
    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 当前 RAG 工作流编排器。
     */
    private final RagWorkflowService ragWorkflowService = new RagWorkflowService();

    public StubChatService(SessionStateRepository sessionStateRepository,
                           HandoffPolicy handoffPolicy,
                           KnowledgeBaseService knowledgeBaseService) {
        this.sessionStateRepository = sessionStateRepository;
        this.handoffPolicy = handoffPolicy;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /**
     * 执行一次流式聊天。
     * <p>
     * 这个方法虽然不长，但已经体现出 application + domain + infrastructure 的协作关系。
     *
     * @param request 聊天请求
     * @return SSE 发射器，后端会通过它持续向前端发事件
     */
    @Override
    public SseEmitter stream(ChatStreamRequest request) {
        SseEmitter emitter = new SseEmitter(0L);

        // 第一步：加载会话状态，并把用户当前消息加入历史记录。
        SessionState state = sessionStateRepository.load(request.threadId());
        state.appendMessage(new ChatMessage("user", request.message()));

        // 第二步：用用户问题去知识库里检索相关来源。
        List<SourceReference> sources = knowledgeBaseService.search(request.message());

        // 第三步：把问题和来源交给 RAG 工作流生成最终答案。
        RagWorkflowResult workflowResult;
        try {
            workflowResult = ragWorkflowService.run(request.message(), sources);
        } catch (Exception exception) {
            throw new IllegalStateException("RAG 工作流执行失败", exception);
        }

        String answer = workflowResult.answer();

        // 第四步：根据当前消息和失败次数判断是否建议转人工。
        boolean handoffSuggested = handoffPolicy.shouldHandoff(
                request.message(),
                state.getConsecutiveFailures()
        );

        // 第五步：更新会话状态，把本轮检索来源、人工接管标记和回答写回去。
        state.setLastSources(sources);
        state.setHandoffSuggested(handoffSuggested);
        state.appendMessage(new ChatMessage("assistant", answer));
        sessionStateRepository.save(state);

        try {
            // 第六步：通过 SSE 逐个发送事件给前端。
            // 这里虽然还是同步一次性发完，但前后端协议已经是流式的了，
            // 后续很容易替换成真正的分片输出。
            emitter.send(SseEmitter.event().name("chunk").data(answer));
            emitter.send(SseEmitter.event().name("sources").data(sources));
            if (handoffSuggested) {
                emitter.send(SseEmitter.event().name("handoff").data("建议转人工处理"));
            }
            emitter.send(SseEmitter.event().name("complete").data("done"));
            emitter.complete();
        } catch (IOException exception) {
            emitter.completeWithError(exception);
        }
        return emitter;
    }
}
