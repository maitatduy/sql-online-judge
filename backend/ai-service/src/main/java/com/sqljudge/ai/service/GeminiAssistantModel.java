package com.sqljudge.ai.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sqljudge.ai.config.AiProperties;
import com.sqljudge.ai.entity.MessageRole;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ServiceUnavailableException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class GeminiAssistantModel implements AssistantModel {

    private static final String API_KEY_HEADER = "x-goog-api-key";
    private static final Set<String> BLOCKED_FINISH_REASONS =
            Set.of("SAFETY", "PROHIBITED_CONTENT", "BLOCKLIST", "SPII", "RECITATION");

    private final RestClient restClient;
    private final AiProperties properties;

    public GeminiAssistantModel(RestClient.Builder builder, AiProperties properties) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(properties.timeout());
        this.restClient =
                builder.baseUrl(properties.baseUrl()).requestFactory(requestFactory).build();
        this.properties = properties;
    }

    @Override
    public AssistantReply reply(String systemPrompt, List<ChatTurn> turns) {
        GenerateRequest request =
                new GenerateRequest(
                        new Content(null, List.of(new Part(systemPrompt))),
                        turns.stream().map(GeminiAssistantModel::contentOf).toList(),
                        new GenerationConfig(properties.maxOutputTokens()));
        try {
            return toReply(
                    restClient
                            .post()
                            .uri("/v1beta/models/{model}:generateContent", properties.model())
                            .header(API_KEY_HEADER, properties.apiKey())
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(request)
                            .retrieve()
                            .body(GenerateResponse.class));
        } catch (RestClientException error) {
            log.warn("llm_call_failed reason={}", error.getClass().getSimpleName());
            throw new ServiceUnavailableException(
                    ErrorCode.AI_UNAVAILABLE, "Trợ lý AI đang bận, vui lòng thử lại sau");
        }
    }

    private static Content contentOf(ChatTurn turn) {
        String role = turn.role() == MessageRole.USER ? "user" : "model";
        return new Content(role, List.of(new Part(turn.content())));
    }

    private static AssistantReply toReply(GenerateResponse response) {
        if (response == null
                || (response.promptFeedback() != null
                        && response.promptFeedback().blockReason() != null)
                || response.candidates() == null
                || response.candidates().isEmpty()) {
            return AssistantReply.refused();
        }
        Candidate candidate = response.candidates().get(0);
        if (BLOCKED_FINISH_REASONS.contains(candidate.finishReason())
                || candidate.content() == null
                || candidate.content().parts() == null) {
            return AssistantReply.refused();
        }
        String text =
                candidate.content().parts().stream()
                        .map(Part::text)
                        .filter(part -> part != null && !part.isBlank())
                        .collect(Collectors.joining());
        return text.isBlank() ? AssistantReply.refused() : AssistantReply.answered(text);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GenerateRequest(
            Content systemInstruction, List<Content> contents, GenerationConfig generationConfig) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Content(String role, List<Part> parts) {}

    record Part(String text) {}

    record GenerationConfig(int maxOutputTokens) {}

    record GenerateResponse(List<Candidate> candidates, PromptFeedback promptFeedback) {}

    record Candidate(Content content, String finishReason) {}

    record PromptFeedback(String blockReason) {}
}
