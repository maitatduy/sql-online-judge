package com.sqljudge.ai.service;

import com.sqljudge.ai.client.AuthServiceClient;
import com.sqljudge.ai.client.UserName;
import com.sqljudge.ai.config.AiProperties;
import com.sqljudge.ai.dto.response.AiFlagResponse;
import com.sqljudge.ai.dto.response.AiSummaryResponse;
import com.sqljudge.ai.dto.response.ConversationDetailResponse;
import com.sqljudge.ai.dto.response.ConversationSummaryResponse;
import com.sqljudge.ai.dto.response.MessageResponse;
import com.sqljudge.ai.entity.AiMessage;
import com.sqljudge.ai.entity.MessageRole;
import com.sqljudge.ai.repository.AiConversationRepository;
import com.sqljudge.ai.repository.AiMessageRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.web.PageResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAiService {

    private static final ZoneId DAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final AiMessageRepository messageRepository;
    private final AiConversationRepository conversationRepository;
    private final AuthServiceClient authServiceClient;
    private final AiProperties properties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AiSummaryResponse summary() {
        Instant since =
                LocalDate.ofInstant(clock.instant(), DAY_ZONE).atStartOfDay(DAY_ZONE).toInstant();
        return new AiSummaryResponse(
                messageRepository.countByRoleAndCreatedAtGreaterThanEqual(MessageRole.USER, since),
                messageRepository.countByBlockedTrueAndCreatedAtGreaterThanEqual(since),
                messageRepository.countUsersSince(MessageRole.USER, since),
                properties.dailyMessageLimit(),
                properties.model());
    }

    public PageResponse<AiFlagResponse> flags(int page, int size) {
        Page<AiMessage> answers =
                messageRepository.findByBlockedTrueOrderByCreatedAtDesc(
                        PageRequest.of(page - 1, size));
        Set<UUID> questionIds =
                answers.stream()
                        .map(AiMessage::getReplyToId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        Map<UUID, String> questions =
                questionIds.isEmpty()
                        ? Map.of()
                        : messageRepository.findByIdIn(questionIds).stream()
                                .collect(Collectors.toMap(AiMessage::getId, AiMessage::getContent));
        Map<UUID, String> usernames =
                usernamesOf(
                        answers.stream()
                                .map(answer -> answer.getConversation().getUserId())
                                .collect(Collectors.toSet()));
        return PageResponse.from(
                answers,
                answer ->
                        new AiFlagResponse(
                                answer.getId(),
                                answer.getConversation().getId(),
                                answer.getConversation().getUserId(),
                                usernames.get(answer.getConversation().getUserId()),
                                answer.getConversation().getProblemSlug(),
                                answer.getBlockReason(),
                                questions.get(answer.getReplyToId()),
                                answer.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public ConversationDetailResponse conversation(UUID conversationId) {
        return conversationRepository
                .findById(conversationId)
                .map(
                        conversation ->
                                new ConversationDetailResponse(
                                        ConversationSummaryResponse.from(conversation),
                                        messageRepository
                                                .findByConversationIdOrderByCreatedAtAscRoleDesc(
                                                        conversationId)
                                                .stream()
                                                .map(MessageResponse::from)
                                                .toList()))
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.CONVERSATION_NOT_FOUND,
                                        "Không tìm thấy hội thoại"));
    }

    private Map<UUID, String> usernamesOf(Set<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return authServiceClient.userNames(userIds).stream()
                .collect(
                        Collectors.toMap(
                                UserName::userId, UserName::username, (first, second) -> first));
    }
}
