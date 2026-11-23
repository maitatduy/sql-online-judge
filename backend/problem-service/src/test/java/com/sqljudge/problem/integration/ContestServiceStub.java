package com.sqljudge.problem.integration;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

final class ContestServiceStub {

    private static final String USAGE_PATH = "/internal/contests/problem-usage/";
    private static final Set<String> PROBLEMS_IN_CONTEST = ConcurrentHashMap.newKeySet();
    private static final AtomicInteger FAILURE_STATUS = new AtomicInteger();
    private static final HttpServer SERVER = start();

    private ContestServiceStub() {}

    static String url() {
        return "http://localhost:" + SERVER.getAddress().getPort();
    }

    static void reset() {
        PROBLEMS_IN_CONTEST.clear();
        FAILURE_STATUS.set(0);
    }

    static void inUnfinishedContest(UUID problemId) {
        PROBLEMS_IN_CONTEST.add(problemId.toString());
    }

    static void failWith(int status) {
        FAILURE_STATUS.set(status);
    }

    private static HttpServer start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext(
                    USAGE_PATH,
                    exchange -> {
                        String problemId =
                                exchange.getRequestURI().getPath().substring(USAGE_PATH.length());
                        int failure = FAILURE_STATUS.get();
                        int status = failure != 0 ? failure : 200;
                        String body =
                                "{\"isInUnfinishedContest\": %s}"
                                        .formatted(PROBLEMS_IN_CONTEST.contains(problemId));
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
}
