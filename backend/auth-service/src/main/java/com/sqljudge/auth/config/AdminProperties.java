package com.sqljudge.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sqljudge.admin")
public record AdminProperties(UserExport userExport, Restrictions restrictions) {

    public record UserExport(int batchSize) {}

    public record Restrictions(int defaultAiDailyQuota) {}
}
