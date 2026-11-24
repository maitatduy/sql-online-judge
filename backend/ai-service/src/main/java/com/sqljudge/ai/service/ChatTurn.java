package com.sqljudge.ai.service;

import com.sqljudge.ai.entity.MessageRole;

public record ChatTurn(MessageRole role, String content) {}
