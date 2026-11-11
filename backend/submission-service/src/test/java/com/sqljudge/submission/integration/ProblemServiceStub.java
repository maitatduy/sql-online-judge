package com.sqljudge.submission.integration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

final class ProblemServiceStub {

    static final String PUBLISHED_ID = "0192f3d0-0000-7000-8000-000000000005";
    static final String REVISION_ID = "0192f3d1-0000-7000-8000-000000000005";
    static final String BROKEN_ID = "0192f3d0-0000-7000-8000-00000000000b";
    private static final String INTERNAL_TOKEN = "test-internal-token";

    private ProblemServiceStub() {}

    static HttpServer start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/internal/problems/", ProblemServiceStub::handle);
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (!INTERNAL_TOKEN.equals(exchange.getRequestHeaders().getFirst("X-Internal-Token"))) {
            respond(exchange, 401, "{\"code\":\"UNAUTHORIZED\"}");
        } else if (path.contains(PUBLISHED_ID)) {
            respond(
                    exchange,
                    200,
                    "{\"problemId\":\""
                            + PUBLISHED_ID
                            + "\",\"revisionId\":\""
                            + REVISION_ID
                            + "\",\"dialects\":[\"POSTGRESQL\",\"SQLITE\"],"
                            + "\"testcaseCount\":3,\"timeLimitMs\":2000}");
        } else if (path.contains(BROKEN_ID)) {
            respond(exchange, 500, "{\"code\":\"INTERNAL_ERROR\"}");
        } else {
            respond(exchange, 404, "{\"code\":\"PROBLEM_NOT_FOUND\"}");
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
