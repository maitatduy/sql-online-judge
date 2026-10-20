package com.sqljudge.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sqljudge.avatar.cloudinary")
public record CloudinaryProperties(
        String cloudName, String apiKey, String apiSecret, String folder, int timeoutSeconds) {}
