package com.example.springrag.application.chat;

import com.example.springrag.domain.chat.SourceReference;
import org.bsc.langgraph4j.state.AgentState;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RagWorkflowState extends AgentState {

    public RagWorkflowState() {
        super(Map.of());
    }

    public RagWorkflowState(Map<String, Object> initData) {
        super(initData);
    }

    public Optional<String> question() {
        return value("question");
    }

    @SuppressWarnings("unchecked")
    public List<SourceReference> sources() {
        return (List<SourceReference>) value("sources").orElse(List.of());
    }

    public Optional<Boolean> retrievalSatisfied() {
        return value("retrievalSatisfied");
    }

    public Optional<String> answer() {
        return value("answer");
    }
}
