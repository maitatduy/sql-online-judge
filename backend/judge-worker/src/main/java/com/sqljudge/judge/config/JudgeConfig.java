package com.sqljudge.judge.config;

import com.sqljudge.judge.guard.StatementGuard;
import com.sqljudge.judge.sandbox.Dialect;
import com.sqljudge.judge.sandbox.MysqlSandbox;
import com.sqljudge.judge.sandbox.MysqlSandboxConfig;
import com.sqljudge.judge.sandbox.PostgresSandbox;
import com.sqljudge.judge.sandbox.PostgresSandboxConfig;
import com.sqljudge.judge.sandbox.SandboxRegistry;
import com.sqljudge.judge.sandbox.SqliteSandbox;
import com.sqljudge.judge.sandbox.SqliteSandboxConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JudgeConfig {

    @Bean
    StatementGuard statementGuard() {
        return new StatementGuard();
    }

    @Bean
    SandboxRegistry sandboxRegistry(SandboxProperties properties) throws IOException {
        SandboxProperties.Postgres postgres = properties.postgres();
        SandboxProperties.Mysql mysql = properties.mysql();
        Files.createDirectories(properties.sqlite().workDirectory());
        return new SandboxRegistry(
                Map.of(
                        Dialect.POSTGRESQL,
                        new PostgresSandbox(
                                new PostgresSandboxConfig(
                                        postgres.jdbcUrl(),
                                        postgres.adminUser(),
                                        postgres.adminPassword())),
                        Dialect.MYSQL,
                        new MysqlSandbox(
                                new MysqlSandboxConfig(
                                        mysql.serverUrl(),
                                        mysql.adminUser(),
                                        mysql.adminPassword())),
                        Dialect.SQLITE,
                        new SqliteSandbox(
                                new SqliteSandboxConfig(properties.sqlite().workDirectory()))));
    }
}
