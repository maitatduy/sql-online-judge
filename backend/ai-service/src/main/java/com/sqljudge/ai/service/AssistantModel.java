package com.sqljudge.ai.service;

import java.util.List;

public interface AssistantModel {

    AssistantReply reply(String systemPrompt, List<ChatTurn> turns);
}
