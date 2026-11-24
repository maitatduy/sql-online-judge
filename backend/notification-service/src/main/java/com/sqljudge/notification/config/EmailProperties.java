package com.sqljudge.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sqljudge.email")
public record EmailProperties(String from, String frontendUrl) {}
