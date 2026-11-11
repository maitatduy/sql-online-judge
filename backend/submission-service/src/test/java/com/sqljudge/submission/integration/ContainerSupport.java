package com.sqljudge.submission.integration;

import com.github.dockerjava.api.command.CreateContainerCmd;
import java.util.function.Consumer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

abstract class ContainerSupport {

    private static final int REDIS_PORT = 6379;
    private static final long BYTES_PER_MEGABYTE = 1024L * 1024;

    @ServiceConnection
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withCommand("--innodb-buffer-pool-size=256M", "--performance-schema=OFF")
                    .withCreateContainerCmdModifier(memoryLimit(768));

    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7.4-alpine")
                    .withExposedPorts(REDIS_PORT)
                    .withCommand("redis-server", "--maxmemory", "64mb")
                    .withCreateContainerCmdModifier(memoryLimit(128));

    static {
        MYSQL.start();
        REDIS.start();
    }

    private static Consumer<CreateContainerCmd> memoryLimit(long megabytes) {
        return command -> command.getHostConfig().withMemory(megabytes * BYTES_PER_MEGABYTE);
    }
}
