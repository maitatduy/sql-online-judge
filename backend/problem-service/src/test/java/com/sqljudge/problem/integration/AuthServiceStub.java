package com.sqljudge.problem.integration;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

final class AuthServiceStub {

    private static final String USERS_PATH = "/internal/users";
    private static final Map<String, String> USERS = new ConcurrentHashMap<>();
    private static final String ACCOUNT_PATH = "/internal/users/by-username/";
    private static final Map<String, String> ACCOUNTS = new ConcurrentHashMap<>();
    private static final AtomicInteger FAILURE_STATUS = new AtomicInteger();
    private static final HttpServer SERVER = start();

    private AuthServiceStub() {}

    static String url() {
        return "http://localhost:" + SERVER.getAddress().getPort();
    }

    static void reset() {
        USERS.clear();
        ACCOUNTS.clear();
        FAILURE_STATUS.set(0);
    }

    static void user(UUID userId, String username) {
        USERS.put(
                userId.toString(),
                """
                {"userId": "%s", "username": "%s", "displayName": "Người %s"}
                """
                        .formatted(userId, username, username));
    }

    static void account(UUID userId, String username, String role, boolean isActive) {
        ACCOUNTS.put(
                username,
                """
                {"userId": "%s", "username": "%s", "role": "%s", "isActive": %s}
                """
                        .formatted(userId, username, role, isActive));
    }

    static void failWith(int status) {
        FAILURE_STATUS.set(status);
    }

    private static HttpServer start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext(
                    USERS_PATH,
                    exchange -> {
                        String path = exchange.getRequestURI().getPath();
                        String account =
                                path.startsWith(ACCOUNT_PATH)
                                        ? ACCOUNTS.get(path.substring(ACCOUNT_PATH.length()))
                                        : null;
                        int failure = FAILURE_STATUS.get();
                        boolean isMissingAccount = path.startsWith(ACCOUNT_PATH) && account == null;
                        int status = failure != 0 ? failure : isMissingAccount ? 404 : 200;
                        String body =
                                status != 200
                                        ? "{}"
                                        : account != null
                                                ? account
                                                : usersOf(exchange.getRequestURI().getRawQuery());
                        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().add("Content-Type", "application/json");
                        exchange.sendResponseHeaders(status, bytes.length);
                        exchange.getResponseBody().write(bytes);
                        exchange.close();
                    });
            server.start();
            return server;
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private static String usersOf(String query) {
        String ids = query == null ? "" : URLDecoder.decode(query, StandardCharsets.UTF_8);
        return Arrays.stream(ids.split("[,&]"))
                .map(part -> part.replace("ids=", ""))
                .map(USERS::get)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(",", "[", "]"));
    }
}
