package com.sqljudge.judge.config;

import com.sqljudge.judge.guard.StatementGuard;
import com.sqljudge.judge.sandbox.Dialect;
import com.sqljudge.judge.sandbox.MysqlSandbox;
import com.sqljudge.judge.sandbox.MysqlSandboxConfig;
import com.sqljudge.judge.sandbox.OrphanSandboxSweeper;
import com.sqljudge.judge.sandbox.PostgresSandbox;
import com.sqljudge.judge.sandbox.PostgresSandboxConfig;
import com.sqljudge.judge.sandbox.SandboxRegistry;
import com.sqljudge.judge.sandbox.SqliteSandbox;
import com.sqljudge.judge.sandbox.SqliteSandboxConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class JudgeConfig {

    @Bean
    StatementGuard statementGuard() {
        return new StatementGuard();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    PostgresSandbox postgresSandbox(SandboxProperties properties, Clock clock) {
        SandboxProperties.Postgres postgres = properties.postgres();
        return new PostgresSandbox(
                new PostgresSandboxConfig(
                        postgres.jdbcUrl(), postgres.adminUser(), postgres.adminPassword()),
                clock);
    }

    @Bean
    MysqlSandbox mysqlSandbox(SandboxProperties properties, Clock clock) {
        SandboxProperties.Mysql mysql = properties.mysql();
        return new MysqlSandbox(
                new MysqlSandboxConfig(mysql.serverUrl(), mysql.adminUser(), mysql.adminPassword()),
                clock);
    }

    @Bean
    SqliteSandbox sqliteSandbox(SandboxProperties properties, Clock clock) throws IOException {
        Files.createDirectories(properties.sqlite().workDirectory());
        return new SqliteSandbox(
                new SqliteSandboxConfig(properties.sqlite().workDirectory()), clock);
    }

    @Bean
    SandboxRegistry sandboxRegistry(
            PostgresSandbox postgresSandbox,
            MysqlSandbox mysqlSandbox,
            SqliteSandbox sqliteSandbox) {
        return new SandboxRegistry(
                Map.of(
                        Dialect.POSTGRESQL, postgresSandbox,
                        Dialect.MYSQL, mysqlSandbox,
                        Dialect.SQLITE, sqliteSandbox));
    }

    @Bean
    OrphanSandboxSweeper orphanSandboxSweeper(
            PostgresSandbox postgresSandbox,
            MysqlSandbox mysqlSandbox,
            SqliteSandbox sqliteSandbox,
            SandboxProperties properties,
            Clock clock) {
        return new OrphanSandboxSweeper(
                List.of(postgresSandbox, mysqlSandbox, sqliteSandbox),
                clock,
                properties.orphanAge());
    }
}
