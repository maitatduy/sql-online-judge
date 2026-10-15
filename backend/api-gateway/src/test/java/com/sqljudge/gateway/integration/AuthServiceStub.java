package com.sqljudge.gateway.integration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

final class AuthServiceStub {

    static final String JWKS_PATH = "/internal/jwks";
    static final String UNAUTHORIZED_BODY =
            "{\"status\":401,\"code\":\"UNAUTHORIZED\",\"message\":\"Bạn cần đăng nhập\"}";

    private final HttpServer server;
    private final String jwksJson;
    private final int status;
    private final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();

    private AuthServiceStub(String jwksJson, int status) throws IOException {
        this.jwksJson = jwksJson;
        this.status = status;
        this.server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    static AuthServiceStub start(String jwksJson, int status) {
        try {
            return new AuthServiceStub(jwksJson, status);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    List<RecordedRequest> forwardedRequests() {
        return requests.stream().filter(request -> !request.uri().equals(JWKS_PATH)).toList();
    }

    List<String> forwardedRequestLines() {
        return forwardedRequests().stream().map(RecordedRequest::line).toList();
    }

    void clear() {
        requests.clear();
    }

    void stop() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.putAll(exchange.getRequestHeaders());
        String uri = exchange.getRequestURI().toString();
        requests.add(new RecordedRequest(exchange.getRequestMethod(), uri, headers));
        boolean isJwks = uri.equals(JWKS_PATH);
        byte[] body = (isJwks ? jwksJson : UNAUTHORIZED_BODY).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders()
                .add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        exchange.sendResponseHeaders(isJwks ? 200 : status, body.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(body);
        }
    }

    record RecordedRequest(String method, String uri, Map<String, List<String>> headers) {

        String line() {
            return method + " " + uri;
        }

        List<String> header(String name) {
            return headers.getOrDefault(name, List.of());
        }
    }
}
