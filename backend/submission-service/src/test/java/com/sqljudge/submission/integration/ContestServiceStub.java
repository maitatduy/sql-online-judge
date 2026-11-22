package com.sqljudge.submission.integration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

final class ContestServiceStub {

    static final String CONTEST_ID = "0193b000-0000-7000-8000-000000000001";
    private static final String ENTRY_PATH = "/internal/contests/";

    private ContestServiceStub() {}

    static HttpServer start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext(ENTRY_PATH, ContestServiceStub::handle);
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals(ENTRY_PATH + "sql-cup/problems/A/entry")) {
            respond(
                    exchange,
                    200,
                    "{\"contestId\":\""
                            + CONTEST_ID
                            + "\",\"problemId\":\""
                            + ProblemServiceStub.PUBLISHED_ID
                            + "\",\"letter\":\"A\"}");
        } else if (path.equals(ENTRY_PATH + "sql-cup/problems/B/entry")) {
            respond(
                    exchange,
                    403,
                    "{\"status\":403,\"code\":\"CONTEST_NOT_REGISTERED\","
                            + "\"message\":\"Bạn cần đăng ký contest để nộp bài\"}");
        } else if (path.equals(ENTRY_PATH + "da-xong/problems/A/entry")) {
            respond(
                    exchange,
                    422,
                    "{\"status\":422,\"code\":\"CONTEST_ALREADY_FINISHED\","
                            + "\"message\":\"Contest đã kết thúc, hãy nộp ở trang luyện tập\"}");
        } else if (path.startsWith(ENTRY_PATH + "loi/")) {
            respond(exchange, 500, "{\"code\":\"INTERNAL_ERROR\"}");
        } else {
            respond(
                    exchange,
                    404,
                    "{\"status\":404,\"code\":\"CONTEST_NOT_FOUND\","
                            + "\"message\":\"Không tìm thấy contest\"}");
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
