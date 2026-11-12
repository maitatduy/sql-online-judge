package com.sqljudge.judge.config;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sqljudge.sandbox")
public record SandboxProperties(Postgres postgres, Mysql mysql, Sqlite sqlite, Duration orphanAge) {

    public record Postgres(String jdbcUrl, String adminUser, String adminPassword) {}

    public record Mysql(String serverUrl, String adminUser, String adminPassword) {}

    public record Sqlite(Path workDirectory) {}
}
