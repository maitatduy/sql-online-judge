package com.sqljudge.auth.integration;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

abstract class ContainerSupport {

    private static final int REDIS_PORT = 6379;

    @ServiceConnection static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7.4-alpine").withExposedPorts(REDIS_PORT);

    static {
        MYSQL.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        MailpitContainer.registerMailProperties(registry);
    }
}
