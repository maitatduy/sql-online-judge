package com.sqljudge.ai.integration;

import com.sqljudge.ai.service.AssistantModel;
import com.sqljudge.ai.service.AssistantReply;
import com.sqljudge.ai.service.ChatTurn;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

class ScriptedAssistantModel implements AssistantModel {

    private AssistantReply nextReply = AssistantReply.answered("Gợi ý: dùng GROUP BY.");
    private final List<String> systemPrompts = new ArrayList<>();
    private final List<List<ChatTurn>> conversations = new ArrayList<>();

    @Override
    public AssistantReply reply(String systemPrompt, List<ChatTurn> turns) {
        systemPrompts.add(systemPrompt);
        conversations.add(List.copyOf(turns));
        return nextReply;
    }

    void willReply(AssistantReply reply) {
        nextReply = reply;
    }

    void reset() {
        nextReply = AssistantReply.answered("Gợi ý: dùng GROUP BY.");
        systemPrompts.clear();
        conversations.clear();
    }

    String lastSystemPrompt() {
        return systemPrompts.get(systemPrompts.size() - 1);
    }

    List<ChatTurn> lastTurns() {
        return conversations.get(conversations.size() - 1);
    }

    @TestConfiguration
    static class Config {

        @Bean
        @Primary
        ScriptedAssistantModel scriptedAssistantModel() {
            return new ScriptedAssistantModel();
        }
    }
}
