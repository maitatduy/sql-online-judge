package com.sqljudge.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sqljudge.jwt")
public record JwtProperties(String privateKeyPath, boolean generateEphemeralKey) {}
