package com.sqljudge.community.integration;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class ProblemServiceStub {

    private static final String PROBLEMS_PATH = "/internal/problems/";
    private static final Set<String> MISSING = ConcurrentHashMap.newKeySet();
    private static final HttpServer SERVER = start();

    private ProblemServiceStub() {}

    static String url() {
        return "http://localhost:" + SERVER.getAddress().getPort();
    }

    static void reset() {
        MISSING.clear();
    }

    static void missing(UUID problemId) {
        MISSING.add(problemId.toString());
    }

    private static HttpServer start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext(
                    PROBLEMS_PATH,
                    exchange -> {
                        String path = exchange.getRequestURI().getPath();
                        String problemId =
                                path.substring(
                                        PROBLEMS_PATH.length(),
                                        path.indexOf('/', PROBLEMS_PATH.length()));
                        boolean isMissing = MISSING.contains(problemId);
                        String body =
                                isMissing
                                        ? "{}"
                                        : "{\"slug\": \"second-highest-salary\", \"title\": \"Lương"
                                                + " cao thứ hai\"}";
                        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().add("Content-Type", "application/json");
                        exchange.sendResponseHeaders(isMissing ? 404 : 200, bytes.length);
                        exchange.getResponseBody().write(bytes);
                        exchange.close();
                    });
            server.start();
            return server;
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }
}
