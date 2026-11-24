package com.sqljudge.ai.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlock;
import com.sqljudge.ai.config.AiProperties;
import com.sqljudge.ai.entity.MessageRole;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ServiceUnavailableException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeAssistantModel implements AssistantModel {

    private static final String FALLBACK_BETA = "server-side-fallback-2026-07-01";

    private final AnthropicClient anthropicClient;
    private final AiProperties properties;

    @Override
    public AssistantReply reply(String systemPrompt, List<ChatTurn> turns) {
        MessageCreateParams.Builder params =
                MessageCreateParams.builder()
                        .model(properties.model())
                        .maxTokens(properties.maxTokens())
                        .system(systemPrompt)
                        .outputConfig(
                                OutputConfig.builder()
                                        .effort(OutputConfig.Effort.of(properties.effort()))
                                        .build())
                        .putAdditionalHeader("anthropic-beta", FALLBACK_BETA)
                        .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
        turns.forEach(
                turn -> {
                    if (turn.role() == MessageRole.USER) {
                        params.addUserMessage(turn.content());
                    } else {
                        params.addAssistantMessage(turn.content());
                    }
                });
        try {
            return toReply(anthropicClient.messages().create(params.build()));
        } catch (AnthropicException error) {
            log.warn("llm_call_failed", error);
            throw new ServiceUnavailableException(
                    ErrorCode.AI_UNAVAILABLE, "Trợ lý AI đang bận, vui lòng thử lại sau");
        }
    }

    private static AssistantReply toReply(Message message) {
        if (message.stopReason().filter(StopReason.REFUSAL::equals).isPresent()) {
            return AssistantReply.refused();
        }
        String text =
                message.content().stream()
                        .flatMap(block -> block.text().stream())
                        .map(TextBlock::text)
                        .collect(Collectors.joining("\n\n"));
        return AssistantReply.answered(text);
    }
}
