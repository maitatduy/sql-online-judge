package com.sqljudge.problem.integration;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;

abstract class ContainerSupport {

    private static final long MYSQL_MEMORY_LIMIT_BYTES = 768L * 1024 * 1024;

    @ServiceConnection
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withCommand("--innodb-buffer-pool-size=256M", "--performance-schema=OFF")
                    .withCreateContainerCmdModifier(
                            command ->
                                    command.getHostConfig().withMemory(MYSQL_MEMORY_LIMIT_BYTES));

    static {
        MYSQL.start();
    }
}
