package com.sqljudge.auth.integration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

final class GithubProviderStub {

    private final HttpServer server;
    private volatile String userJson = "{}";
    private volatile String emailsJson = "[]";

    private GithubProviderStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext(
                "/login/oauth/access_token",
                exchange ->
                        respond(
                                exchange,
                                "{\"access_token\":\"gho_test\",\"token_type\":\"bearer\","
                                        + "\"scope\":\"read:user,user:email\"}"));
        server.createContext("/user/emails", exchange -> respond(exchange, emailsJson));
        server.createContext("/user", exchange -> respond(exchange, userJson));
        server.start();
    }

    static GithubProviderStub start() {
        try {
            return new GithubProviderStub();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    void willReturn(long id, String login, String email, boolean isEmailVerified) {
        userJson = "{\"id\":%d,\"login\":\"%s\",\"name\":\"%s\"}".formatted(id, login, login);
        emailsJson =
                "[{\"email\":\"%s\",\"primary\":true,\"verified\":%s}]"
                        .formatted(email, isEmailVerified);
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
