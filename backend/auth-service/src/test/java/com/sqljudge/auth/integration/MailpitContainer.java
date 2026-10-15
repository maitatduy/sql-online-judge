package com.sqljudge.auth.integration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;

final class MailpitContainer {

    static final Pattern CODE = Pattern.compile("\\b(\\d{6})\\b");
    static final Pattern LINK =
            Pattern.compile("http://localhost:3000/verify-email\\?token=([A-Za-z0-9_-]{43})");

    private static final int SMTP_PORT = 1025;
    private static final int API_PORT = 8025;
    private static final GenericContainer<?> CONTAINER =
            new GenericContainer<>("axllent/mailpit:v1.27").withExposedPorts(SMTP_PORT, API_PORT);

    static {
        CONTAINER.start();
    }

    private MailpitContainer() {}

    static void registerMailProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", CONTAINER::getHost);
        registry.add("spring.mail.port", () -> CONTAINER.getMappedPort(SMTP_PORT));
    }

    static MailpitClient client() {
        return new MailpitClient(
                "http://" + CONTAINER.getHost() + ":" + CONTAINER.getMappedPort(API_PORT));
    }

    static String firstGroup(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            throw new AssertionError("Pattern " + pattern + " not found in mail: " + text);
        }
        return matcher.group(1);
    }
}
