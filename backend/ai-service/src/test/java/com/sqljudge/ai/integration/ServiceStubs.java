package com.sqljudge.ai.integration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

final class ServiceStubs {

    static final UUID PROBLEM_ID = UUID.fromString("0192f3b0-0000-7000-8000-000000000184");
    static final UUID POST_ID = UUID.fromString("0192f3a0-0000-7000-9000-000000000002");
    private static final String PROBLEMS_PATH = "/internal/problems/";
    private static final String CONTESTS_PATH = "/internal/contests/rated-participation/";
    private static final Set<String> SOLVERS = ConcurrentHashMap.newKeySet();
    private static final Set<String> CONTESTANTS = ConcurrentHashMap.newKeySet();
    private static final HttpServer SERVER = start();

    private ServiceStubs() {}

    static String url() {
        return "http://localhost:" + SERVER.getAddress().getPort();
    }

    static void reset() {
        SOLVERS.clear();
        CONTESTANTS.clear();
    }

    static void solved(UUID userId) {
        SOLVERS.add(userId.toString());
    }

    static void inRatedContest(UUID userId) {
        CONTESTANTS.add(userId.toString());
    }

    private static HttpServer start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext(PROBLEMS_PATH, ServiceStubs::problem);
            server.createContext(CONTESTS_PATH, ServiceStubs::participation);
            server.createContext("/internal/users", ServiceStubs::userNames);
            server.createContext("/api/community/posts/", ServiceStubs::communityPost);
            server.createContext("/api/community/comments", ServiceStubs::communityComments);
            server.start();
            return server;
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void communityPost(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestURI().getPath().endsWith(POST_ID.toString())) {
            respond(exchange, 404, "{\"code\":\"POST_NOT_FOUND\"}");
            return;
        }
        respond(
                exchange,
                200,
                "{\"title\": \"COUNT(*) hay COUNT(cột)?\", \"bodyMarkdown\": \"Khác nhau chỗ"
                        + " nào?\"}");
    }

    private static void communityComments(HttpExchange exchange) throws IOException {
        respond(
                exchange,
                200,
                """
                {"items": [{"authorUsername": "an.bui", "body": "COUNT(cột) bỏ qua NULL.",
                  "replies": [{"authorUsername": "minh.anh", "body": "Cảm ơn!", "replies": []}]}]}
                """);
    }

    private static void userNames(HttpExchange exchange) throws IOException {
        String ids = exchange.getRequestURI().getQuery().replace("ids=", "");
        String body =
                Arrays.stream(ids.split("[,&]"))
                        .map(id -> "{\"userId\": \"%s\", \"username\": \"hoc.vien\"}".formatted(id))
                        .collect(Collectors.joining(",", "[", "]"));
        respond(exchange, 200, body);
    }

    private static void problem(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (!path.startsWith(PROBLEMS_PATH + PROBLEM_ID)) {
            respond(exchange, 404, "{\"code\":\"PROBLEM_NOT_FOUND\"}");
            return;
        }
        String userId = exchange.getRequestURI().getQuery().replace("userId=", "");
        respond(
                exchange,
                200,
                """
                {"problemId": "%s", "slug": "highest-salary", "title": "Lương cao nhất",
                 "statementMarkdown": "Tìm lương cao nhất mỗi phòng ban.",
                 "dialects": ["POSTGRESQL"],
                 "schema": [{"name": "employees", "columns": [{"name": "salary", "type": "INTEGER"}]}],
                 "isSolved": %s}
                """
                        .formatted(PROBLEM_ID, SOLVERS.contains(userId)));
    }

    private static void participation(HttpExchange exchange) throws IOException {
        String userId = exchange.getRequestURI().getPath().substring(CONTESTS_PATH.length());
        respond(
                exchange,
                200,
                "{\"isInRatedContest\": %s}".formatted(CONTESTANTS.contains(userId)));
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
