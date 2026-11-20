package com.sqljudge.learning.integration;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

final class ProblemServiceStub {

    private static final String VARIANTS_PATH = "/internal/dataset-variants/";
    private static final Map<String, Response> RESPONSES = new ConcurrentHashMap<>();
    private static final AtomicReference<String> RECEIVED_TOKEN = new AtomicReference<>();
    private static final HttpServer SERVER = start();

    private ProblemServiceStub() {}

    static String url() {
        return "http://localhost:" + SERVER.getAddress().getPort();
    }

    static void respond(UUID variantId, int status, String body) {
        RESPONSES.put(variantId.toString(), new Response(status, body));
    }

    static void variant(UUID variantId, UUID versionId, boolean isPublic, boolean isFrozen) {
        String checksum = isFrozen ? "\"" + checksumOf(versionId) + "\"" : "null";
        respond(
                variantId,
                200,
                """
                {"variantId": "%s", "versionId": "%s", "datasetName": "Công ty",
                 "variantName": "Mẫu", "isPublic": %s, "isFrozen": %s, "checksum": %s}
                """
                        .formatted(variantId, versionId, isPublic, isFrozen, checksum));
    }

    static String checksumOf(UUID versionId) {
        return versionId.toString().replace("-", "").repeat(2);
    }

    static String receivedToken() {
        return RECEIVED_TOKEN.get();
    }

    private static HttpServer start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext(
                    VARIANTS_PATH,
                    exchange -> {
                        RECEIVED_TOKEN.set(
                                exchange.getRequestHeaders().getFirst("X-Internal-Token"));
                        String variantId =
                                exchange.getRequestURI()
                                        .getPath()
                                        .substring(VARIANTS_PATH.length());
                        Response response =
                                RESPONSES.getOrDefault(variantId, new Response(404, "{}"));
                        byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().add("Content-Type", "application/json");
                        exchange.sendResponseHeaders(response.status(), body.length);
                        exchange.getResponseBody().write(body);
                        exchange.close();
                    });
            server.start();
            return server;
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private record Response(int status, String body) {}
}
