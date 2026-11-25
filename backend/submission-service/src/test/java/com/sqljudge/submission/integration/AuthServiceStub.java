package com.sqljudge.submission.integration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

final class AuthServiceStub {

    private static final String BY_USERNAME_PATH = "/internal/users/by-username/";
    private static final Map<String, String> NAMES_BY_ID = new ConcurrentHashMap<>();
    private static final Map<String, String> ACCOUNTS_BY_USERNAME = new ConcurrentHashMap<>();
    private static final HttpServer SERVER = start();

    private AuthServiceStub() {}

    static String url() {
        return "http://localhost:" + SERVER.getAddress().getPort();
    }

    static void reset() {
        NAMES_BY_ID.clear();
        ACCOUNTS_BY_USERNAME.clear();
    }

    static void user(UUID userId, String username) {
        NAMES_BY_ID.put(
                userId.toString(),
                """
                {"userId": "%s", "username": "%s", "displayName": "Người %s"}
                """
                        .formatted(userId, username, username));
        ACCOUNTS_BY_USERNAME.put(
                username,
                """
                {"userId": "%s", "username": "%s", "role": "LEARNER", "isActive": true}
                """
                        .formatted(userId, username));
    }

    private static HttpServer start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext(
                    BY_USERNAME_PATH,
                    exchange -> {
                        String username =
                                exchange.getRequestURI()
                                        .getPath()
                                        .substring(BY_USERNAME_PATH.length());
                        String account = ACCOUNTS_BY_USERNAME.get(username);
                        respond(exchange, account == null ? 404 : 200, account);
                    });
            server.createContext(
                    "/internal/users",
                    exchange -> {
                        String query = exchange.getRequestURI().getQuery();
                        String body =
                                Arrays.stream(query.replace("ids=", "").split("[,&]"))
                                        .map(NAMES_BY_ID::get)
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.joining(",", "[", "]"));
                        respond(exchange, 200, body);
                    });
            server.start();
            return server;
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = (body == null ? "{}" : body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
