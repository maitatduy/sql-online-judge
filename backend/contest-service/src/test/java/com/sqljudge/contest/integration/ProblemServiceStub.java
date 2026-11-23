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
    private static final String PROBLEMS_PATH = "/internal/problems/";
    private static final String STATEMENT_SUFFIX = "/contest-statement";
    private static final Map<String, String> STATEMENTS = new ConcurrentHashMap<>();
    private static final Map<String, String> READY_PROBLEMS = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> STATEMENT_REQUESTS = new ConcurrentHashMap<>();
    private static final AtomicInteger FAILURE_STATUS = new AtomicInteger();
    private static final AtomicReference<String> RECEIVED_TOKEN = new AtomicReference<>();
    private static final HttpServer SERVER = start();

    private ProblemServiceStub() {}

    static String url() {
        return "http://localhost:" + SERVER.getAddress().getPort();
    }

    static void reset() {
        READY_PROBLEMS.clear();
        STATEMENTS.clear();
        STATEMENT_REQUESTS.clear();
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

    static void statement(UUID problemId, String title) {
        STATEMENTS.put(
                problemId.toString(),
                """
                {"title": "%s", "difficulty": "MEDIUM", "statementMarkdown": "Liệt kê phòng ban.",
                 "dialects": ["POSTGRESQL"], "engineVersions": {"POSTGRESQL": "16"},
                 "starterCode": "SELECT", "schema": [], "examples": [], "requiresOrder": false,
                 "allowedStatements": ["SELECT"], "timeLimitMs": 2000, "hiddenTestcaseCount": 3}
                """
                        .formatted(title));
    }

    static void failWith(int status) {
        FAILURE_STATUS.set(status);
    }

    static int statementRequests(UUID problemId) {
        AtomicInteger requests = STATEMENT_REQUESTS.get(problemId.toString());
        return requests == null ? 0 : requests.get();
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
            server.createContext(
                    PROBLEMS_PATH,
                    exchange -> {
                        String path = exchange.getRequestURI().getPath();
                        String problemId =
                                path.substring(
                                        PROBLEMS_PATH.length(),
                                        path.length() - STATEMENT_SUFFIX.length());
                        STATEMENT_REQUESTS
                                .computeIfAbsent(problemId, key -> new AtomicInteger())
                                .incrementAndGet();
                        String statement = STATEMENTS.get(problemId);
                        byte[] body =
                                (statement == null ? "{}" : statement)
                                        .getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().add("Content-Type", "application/json");
                        exchange.sendResponseHeaders(statement == null ? 404 : 200, body.length);
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
