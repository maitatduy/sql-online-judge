package com.sqljudge.submission.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sqljudge.internal")
public record InternalApiProperties(String apiToken) {}
