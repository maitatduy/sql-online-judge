package com.sqljudge.problem.integration;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

abstract class ContainerSupport {

    private static final long MYSQL_MEMORY_LIMIT_BYTES = 768L * 1024 * 1024;
    private static final long REDIS_MEMORY_LIMIT_BYTES = 128L * 1024 * 1024;
    private static final int REDIS_PORT = 6379;

    @ServiceConnection
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withCommand("--innodb-buffer-pool-size=256M", "--performance-schema=OFF")
                    .withCreateContainerCmdModifier(
                            command ->
                                    command.getHostConfig().withMemory(MYSQL_MEMORY_LIMIT_BYTES));

    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7.4-alpine")
                    .withExposedPorts(REDIS_PORT)
                    .withCommand("redis-server", "--maxmemory", "64mb")
                    .withCreateContainerCmdModifier(
                            command ->
                                    command.getHostConfig().withMemory(REDIS_MEMORY_LIMIT_BYTES));

    static {
        MYSQL.start();
        REDIS.start();
    }
}
