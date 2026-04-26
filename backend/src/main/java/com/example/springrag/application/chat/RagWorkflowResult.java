package com.example.springrag.application.chat;

public record RagWorkflowResult(String answer, boolean retrievalSatisfied) {
}
