package com.sqljudge.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sqljudge.ai")
public record AiProperties(
        String apiKey,
        String model,
        int dailyMessageLimit,
        String effort,
        long maxTokens,
        int historyMessages) {}
