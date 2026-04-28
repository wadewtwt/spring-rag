package com.example.springrag.infrastructure.chat;

import com.example.springrag.api.chat.ChatStreamRequest;
import com.example.springrag.application.chat.ChatService;
import com.example.springrag.application.chat.HandoffPolicy;
import com.example.springrag.application.chat.RagWorkflowResult;
import com.example.springrag.application.chat.RagWorkflowService;
import com.example.springrag.application.chat.SessionStateRepository;
import com.example.springrag.domain.chat.ChatMessage;
import com.example.springrag.domain.chat.SessionState;
import com.example.springrag.domain.chat.SourceReference;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Service
public class StubChatService implements ChatService {

    private final SessionStateRepository sessionStateRepository;
    private final HandoffPolicy handoffPolicy;
    private final RagWorkflowService ragWorkflowService;

    public StubChatService(SessionStateRepository sessionStateRepository,
                           HandoffPolicy handoffPolicy,
                           RagWorkflowService ragWorkflowService) {
        this.sessionStateRepository = sessionStateRepository;
        this.handoffPolicy = handoffPolicy;
        this.ragWorkflowService = ragWorkflowService;
    }

    @Override
    public SseEmitter stream(ChatStreamRequest request) {
        SseEmitter emitter = new SseEmitter(0L);

        SessionState state = sessionStateRepository.load(request.threadId());
        state.appendMessage(new ChatMessage("user", request.message()));

        RagWorkflowResult workflowResult;
        try {
            workflowResult = ragWorkflowService.run(request.message());
        } catch (Exception exception) {
            throw new IllegalStateException("RAG workflow execution failed", exception);
        }

        String answer = workflowResult.answer();
        List<SourceReference> sources = workflowResult.sources();
        boolean handoffSuggested = handoffPolicy.shouldHandoff(
                request.message(),
                state.getConsecutiveFailures()
        );

        state.setLastSources(sources);
        state.setHandoffSuggested(handoffSuggested);
        state.appendMessage(new ChatMessage("assistant", answer));
        sessionStateRepository.save(state);

        try {
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
