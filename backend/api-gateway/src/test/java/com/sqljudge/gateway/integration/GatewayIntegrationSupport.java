package com.sqljudge.gateway.integration;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;

abstract class GatewayIntegrationSupport {

    private static final int REDIS_PORT = 6379;
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(30);
    private static final long REDIS_MEMORY_LIMIT_BYTES = 128L * 1024 * 1024;

    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7.4-alpine")
                    .withExposedPorts(REDIS_PORT)
                    .withCommand(
                            "redis-server",
                            "--maxmemory",
                            "64mb",
                            "--maxmemory-policy",
                            "noeviction")
                    .withCreateContainerCmdModifier(
                            command ->
                                    command.getHostConfig().withMemory(REDIS_MEMORY_LIMIT_BYTES));

    static final AuthServiceStub PROBLEM_SERVICE = AuthServiceStub.start("{\"keys\":[]}", 404);

    static {
        REDIS.start();
    }

    protected WebTestClient webTestClient;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("REDIS_HOST", REDIS::getHost);
        registry.add("REDIS_PORT", () -> REDIS.getMappedPort(REDIS_PORT));
        registry.add("REDIS_PASSWORD", () -> "");
        registry.add("PROBLEM_SERVICE_URL", PROBLEM_SERVICE::baseUrl);
    }

    @BeforeEach
    void clearProblemServiceRequests() {
        PROBLEM_SERVICE.clear();
    }

    @Autowired
    void useServerWebTestClient(WebTestClient serverWebTestClient) {
        this.webTestClient = serverWebTestClient.mutate().responseTimeout(RESPONSE_TIMEOUT).build();
    }
}
