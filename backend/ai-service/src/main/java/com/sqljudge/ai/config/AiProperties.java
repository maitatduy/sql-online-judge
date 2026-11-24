package com.sqljudge.ai.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sqljudge.ai")
public record AiProperties(
        String apiKey,
        String model,
        String baseUrl,
        int dailyMessageLimit,
        int maxOutputTokens,
        Duration timeout,
        int historyMessages) {}
