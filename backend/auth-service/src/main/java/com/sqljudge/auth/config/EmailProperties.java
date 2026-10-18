package com.sqljudge.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sqljudge.email")
public record EmailProperties(String from, String frontendUrl) {}
