package com.sqljudge.auth.integration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

final class GoogleProviderStub {

    private final HttpServer server;
    private volatile String userInfoJson = "{}";

    private GoogleProviderStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext(
                "/token",
                exchange ->
                        respond(
                                exchange,
                                "{\"access_token\":\"ya29.test\",\"token_type\":\"Bearer\","
                                        + "\"expires_in\":3599,\"scope\":\"profile email\"}"));
        server.createContext("/v3/userinfo", exchange -> respond(exchange, userInfoJson));
        server.start();
    }

    static GoogleProviderStub start() {
        try {
            return new GoogleProviderStub();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    void willReturn(String subject, String email, boolean isEmailVerified, String name) {
        userInfoJson =
                "{\"sub\":\"%s\",\"email\":\"%s\",\"email_verified\":%s,\"name\":\"%s\"}"
                        .formatted(subject, email, isEmailVerified, name);
    }

    private static void respond(HttpExchange exchange, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }
}
