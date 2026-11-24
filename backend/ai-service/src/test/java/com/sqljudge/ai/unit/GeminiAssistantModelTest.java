package com.sqljudge.ai.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sqljudge.ai.config.AiProperties;
import com.sqljudge.ai.entity.MessageRole;
import com.sqljudge.ai.service.AssistantReply;
import com.sqljudge.ai.service.ChatTurn;
import com.sqljudge.ai.service.GeminiAssistantModel;
import com.sqljudge.common.exception.ServiceUnavailableException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class GeminiAssistantModelTest {

    private static final List<ChatTurn> TURNS =
            List.of(
                    new ChatTurn(MessageRole.USER, "Bài này dùng gì?"),
                    new ChatTurn(MessageRole.ASSISTANT, "GROUP BY."),
                    new ChatTurn(MessageRole.USER, "Vì sao?"));

    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> requestPath = new AtomicReference<>();
    private final AtomicReference<String> apiKey = new AtomicReference<>();
    private HttpServer server;
    private int status;
    private String responseBody;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext(
                "/",
                exchange -> {
                    requestPath.set(exchange.getRequestURI().getPath());
                    apiKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
                    requestBody.set(
                            new String(
                                    exchange.getRequestBody().readAllBytes(),
                                    StandardCharsets.UTF_8));
                    byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(status, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.close();
                });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    @DisplayName(
            "Gửi system instruction, lịch sử theo vai user/model, khoá ở header; ghép các đoạn trả"
                    + " lời")
    void reply_sendsConversationAndJoinsParts() {
        respond(
                200,
                """
                {"candidates": [{"content": {"role": "model",
                  "parts": [{"text": "Vì cần gom "}, {"text": "theo phòng ban."}]},
                  "finishReason": "STOP"}]}
                """);

        AssistantReply reply = model().reply("Chỉ gợi ý.", TURNS);

        assertThat(reply).isEqualTo(AssistantReply.answered("Vì cần gom theo phòng ban."));
        assertThat(requestPath.get()).isEqualTo("/v1beta/models/gemini-test:generateContent");
        assertThat(apiKey.get()).isEqualTo("secret-key");
        assertThat(requestBody.get())
                .contains("\"systemInstruction\":{\"parts\":[{\"text\":\"Chỉ gợi ý.\"}]}")
                .contains("{\"role\":\"model\",\"parts\":[{\"text\":\"GROUP BY.\"}]}")
                .contains("\"maxOutputTokens\":512");
    }

    @Test
    @DisplayName("Bị chặn vì an toàn hoặc không có câu trả lời thì coi là từ chối")
    void reply_blockedOrEmpty_isRefused() {
        respond(200, "{\"candidates\": [{\"finishReason\": \"SAFETY\"}]}");
        assertThat(model().reply("x", TURNS).isRefused()).isTrue();

        respond(200, "{\"promptFeedback\": {\"blockReason\": \"OTHER\"}}");
        assertThat(model().reply("x", TURNS).isRefused()).isTrue();
    }

    @Test
    @DisplayName("Gemini lỗi hoặc hết hạn mức thì báo trợ lý AI đang bận")
    void reply_upstreamError_isServiceUnavailable() {
        respond(429, "{\"error\": {\"status\": \"RESOURCE_EXHAUSTED\"}}");

        assertThatThrownBy(() -> model().reply("x", TURNS))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    private void respond(int nextStatus, String body) {
        status = nextStatus;
        responseBody = body;
    }

    private GeminiAssistantModel model() {
        return new GeminiAssistantModel(
                RestClient.builder(),
                new AiProperties(
                        "secret-key",
                        "gemini-test",
                        "http://localhost:" + server.getAddress().getPort(),
                        10,
                        512,
                        Duration.ofSeconds(5),
                        20));
    }
}
