package com.sqljudge.auth.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.StreamSupport;

final class MailpitClient {

    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(200);

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl;

    MailpitClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    ReceivedMail awaitMailTo(String recipient) throws IOException, InterruptedException {
        Instant deadline = Instant.now().plus(POLL_TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            Optional<String> messageId = findMessageIdTo(recipient);
            if (messageId.isPresent()) {
                JsonNode message = get("/api/v1/message/" + messageId.get());
                return new ReceivedMail(
                        message.path("Subject").asText(), message.path("Text").asText());
            }
            Thread.sleep(POLL_INTERVAL.toMillis());
        }
        throw new AssertionError("No mail delivered to " + recipient + " within " + POLL_TIMEOUT);
    }

    private Optional<String> findMessageIdTo(String recipient)
            throws IOException, InterruptedException {
        JsonNode messages = get("/api/v1/messages").path("messages");
        return StreamSupport.stream(messages.spliterator(), false)
                .filter(message -> isAddressedTo(message, recipient))
                .map(message -> message.path("ID").asText())
                .findFirst();
    }

    private static boolean isAddressedTo(JsonNode message, String recipient) {
        return StreamSupport.stream(message.path("To").spliterator(), false)
                .anyMatch(to -> recipient.equalsIgnoreCase(to.path("Address").asText()));
    }

    private JsonNode get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build();
        String body = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
        return objectMapper.readTree(body);
    }

    record ReceivedMail(String subject, String text) {}
}
