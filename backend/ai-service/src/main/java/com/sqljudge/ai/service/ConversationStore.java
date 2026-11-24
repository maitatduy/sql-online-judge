package com.sqljudge.ai.service;

import com.sqljudge.ai.config.AiProperties;
import com.sqljudge.ai.dto.response.ConversationDetailResponse;
import com.sqljudge.ai.dto.response.ConversationSummaryResponse;
import com.sqljudge.ai.dto.response.MessageResponse;
import com.sqljudge.ai.entity.AiConversation;
import com.sqljudge.ai.entity.AiConversation.ProblemRef;
import com.sqljudge.ai.entity.AiMessage;
import com.sqljudge.ai.entity.BlockReason;
import com.sqljudge.ai.repository.AiConversationRepository;
import com.sqljudge.ai.repository.AiMessageRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.web.PageResponse;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationStore {

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final AiProperties properties;
    private final Clock clock;

    @Transactional
    public ConversationSummaryResponse start(UUID userId, ProblemRef problem) {
        return ConversationSummaryResponse.from(
                conversationRepository.save(
                        AiConversation.start(userId, problem, clock.instant())));
    }

    @Transactional(readOnly = true)
    public PageResponse<ConversationSummaryResponse> list(UUID userId, int page, int size) {
        return PageResponse.from(
                conversationRepository.findByUserIdOrderByLastMessageAtDesc(
                        userId, PageRequest.of(page - 1, size)),
                ConversationSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public ConversationDetailResponse detail(UUID conversationId, UUID userId) {
        AiConversation conversation = owned(conversationId, userId);
        return new ConversationDetailResponse(
                ConversationSummaryResponse.from(conversation),
                messageRepository
                        .findByConversationIdOrderByCreatedAtAscRoleDesc(conversationId)
                        .stream()
                        .map(MessageResponse::from)
                        .toList());
    }

    @Transactional(readOnly = true)
    public ConversationSnapshot snapshot(UUID conversationId, UUID userId) {
        AiConversation conversation = owned(conversationId, userId);
        List<ChatTurn> history =
                new ArrayList<>(
                        messageRepository
                                .findByConversationIdOrderByCreatedAtDescRoleAsc(
                                        conversationId,
                                        PageRequest.of(0, properties.historyMessages()))
                                .stream()
                                .map(
                                        message ->
                                                new ChatTurn(
                                                        message.getRole(), message.getContent()))
                                .toList());
        Collections.reverse(history);
        return new ConversationSnapshot(conversation.getProblemId(), history);
    }

    @Transactional
    public Exchange append(
            UUID conversationId,
            UUID userId,
            String question,
            String answer,
            BlockReason blockReason) {
        AiConversation conversation = owned(conversationId, userId);
        boolean isFirstMessage = !messageRepository.existsByConversationId(conversationId);
        AiMessage asked = messageRepository.save(AiMessage.question(conversation, question));
        AiMessage answered =
                messageRepository.save(AiMessage.answer(conversation, asked, answer, blockReason));
        conversation.messageAdded(question, isFirstMessage, clock.instant());
        messageRepository.flush();
        return new Exchange(MessageResponse.from(asked), MessageResponse.from(answered));
    }

    private AiConversation owned(UUID conversationId, UUID userId) {
        return conversationRepository
                .findById(conversationId)
                .filter(conversation -> conversation.isOwnedBy(userId))
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.CONVERSATION_NOT_FOUND,
                                        "Không tìm thấy hội thoại"));
    }

    public record ConversationSnapshot(UUID problemId, List<ChatTurn> history) {}

    public record Exchange(MessageResponse question, MessageResponse answer) {}
}
