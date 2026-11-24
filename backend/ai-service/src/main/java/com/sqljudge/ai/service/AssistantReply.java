package com.sqljudge.ai.service;

public record AssistantReply(String text, boolean isRefused) {

    public static AssistantReply answered(String text) {
        return new AssistantReply(text, false);
    }

    public static AssistantReply refused() {
        return new AssistantReply("", true);
    }
}
