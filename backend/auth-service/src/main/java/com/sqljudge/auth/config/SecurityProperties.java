package com.sqljudge.auth.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sqljudge.security")
public record SecurityProperties(List<String> allowedOrigins) {

    public SecurityProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new IllegalStateException(
                    "sqljudge.security.allowed-origins (CORS_ALLOWED_ORIGINS) must not be empty");
        }
        allowedOrigins = List.copyOf(allowedOrigins);
    }
}
