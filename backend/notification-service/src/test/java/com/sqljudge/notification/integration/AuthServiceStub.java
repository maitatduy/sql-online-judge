package com.sqljudge.notification.integration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

final class AuthServiceStub {

    private static final String USERS_PATH = "/internal/users/";
    private static final List<UUID> ACTIVE_USERS = new CopyOnWriteArrayList<>();
    private static final HttpServer SERVER = start();

    private AuthServiceStub() {}

    static String url() {
        return "http://localhost:" + SERVER.getAddress().getPort();
    }

    static void activeUsers(List<UUID> userIds) {
        ACTIVE_USERS.clear();
        ACTIVE_USERS.addAll(userIds.stream().sorted().toList());
    }

    private static HttpServer start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext(USERS_PATH, AuthServiceStub::handle);
            server.start();
            return server;
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.endsWith("/active-ids")) {
            respond(exchange, activeIds(exchange.getRequestURI().getQuery()));
            return;
        }
        String userId = path.substring(USERS_PATH.length(), path.indexOf("/contact"));
        respond(
                exchange,
                """
                {"userId": "%s", "username": "hoc.vien", "email": "hoc.vien@gmail.com",
                 "isEmailVerified": true}
                """
                        .formatted(userId));
    }

    private static String activeIds(String query) {
        UUID afterId = null;
        int size = 500;
        for (String part : query.split("&")) {
            String[] pair = part.split("=");
            if (pair[0].equals("afterId")) {
                afterId = UUID.fromString(pair[1]);
            } else if (pair[0].equals("size")) {
                size = Integer.parseInt(pair[1]);
            }
        }
        UUID after = afterId;
        return ACTIVE_USERS.stream()
                .filter(id -> after == null || id.compareTo(after) > 0)
                .limit(size)
                .map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
