package com.sqljudge.contest.integration;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

final class AuthServiceStub {

    private static final String USERS_PATH = "/internal/users/";
    private static final Map<String, String> USERS = new ConcurrentHashMap<>();
    private static final AtomicInteger FAILURE_STATUS = new AtomicInteger();
    private static final HttpServer SERVER = start();

    private AuthServiceStub() {}

    static String url() {
        return "http://localhost:" + SERVER.getAddress().getPort();
    }

    static void reset() {
        USERS.clear();
        FAILURE_STATUS.set(0);
    }

    static void user(UUID userId, String username, boolean isBannedFromRatedContests) {
        USERS.put(
                userId.toString(),
                """
                {"userId": "%s", "username": "%s", "displayName": "Người %s",
                 "isBannedFromRatedContests": %s}
                """
                        .formatted(userId, username, username, isBannedFromRatedContests));
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
                        String userId =
                                exchange.getRequestURI().getPath().substring(USERS_PATH.length());
                        String user = USERS.get(userId);
                        int failure = FAILURE_STATUS.get();
                        int status = failure != 0 ? failure : user == null ? 404 : 200;
                        byte[] body =
                                (status == 200 ? user : "{}").getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().add("Content-Type", "application/json");
                        exchange.sendResponseHeaders(status, body.length);
                        exchange.getResponseBody().write(body);
                        exchange.close();
                    });
            server.start();
            return server;
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }
}
