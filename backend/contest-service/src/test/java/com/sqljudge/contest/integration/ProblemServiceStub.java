package com.sqljudge.contest.integration;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

final class ProblemServiceStub {

    private static final String CONTEST_READY_PATH = "/internal/problems/contest-ready";
    private static final Map<String, String> READY_PROBLEMS = new ConcurrentHashMap<>();
    private static final AtomicInteger FAILURE_STATUS = new AtomicInteger();
    private static final AtomicReference<String> RECEIVED_TOKEN = new AtomicReference<>();
    private static final HttpServer SERVER = start();

    private ProblemServiceStub() {}

    static String url() {
        return "http://localhost:" + SERVER.getAddress().getPort();
    }

    static void reset() {
        READY_PROBLEMS.clear();
        FAILURE_STATUS.set(0);
        RECEIVED_TOKEN.set(null);
    }

    static void ready(UUID problemId, String slug, String title) {
        READY_PROBLEMS.put(
                problemId.toString(),
                """
                {"problemId": "%s", "slug": "%s", "title": "%s", "difficulty": "MEDIUM"}
                """
                        .formatted(problemId, slug, title));
    }

    static void failWith(int status) {
        FAILURE_STATUS.set(status);
    }

    static String receivedToken() {
        return RECEIVED_TOKEN.get();
    }

    private static HttpServer start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext(
                    CONTEST_READY_PATH,
                    exchange -> {
                        RECEIVED_TOKEN.set(
                                exchange.getRequestHeaders().getFirst("X-Internal-Token"));
                        int failure = FAILURE_STATUS.get();
                        String query = exchange.getRequestURI().getQuery();
                        String ids = query == null ? "" : query.replace("ids=", "");
                        byte[] body =
                                (failure == 0 ? readyProblems(ids) : "{}")
                                        .getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().add("Content-Type", "application/json");
                        exchange.sendResponseHeaders(failure == 0 ? 200 : failure, body.length);
                        exchange.getResponseBody().write(body);
                        exchange.close();
                    });
            server.start();
            return server;
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private static String readyProblems(String ids) {
        return Arrays.stream(ids.split("[,&]"))
                .map(READY_PROBLEMS::get)
                .filter(problem -> problem != null)
                .collect(Collectors.joining(",", "[", "]"));
    }
}
