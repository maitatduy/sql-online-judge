package com.sqljudge.ai.service;

import com.sqljudge.ai.client.ProblemContext;
import com.sqljudge.ai.client.ProblemServiceClient;
import com.sqljudge.ai.dto.request.SendMessageRequest;
import com.sqljudge.ai.dto.response.ConversationSummaryResponse;
import com.sqljudge.ai.dto.response.MessageExchangeResponse;
import com.sqljudge.ai.entity.AiConversation.ProblemRef;
import com.sqljudge.ai.entity.BlockReason;
import com.sqljudge.ai.entity.MessageRole;
import com.sqljudge.ai.service.ConversationStore.ConversationSnapshot;
import com.sqljudge.ai.service.ConversationStore.Exchange;
import com.sqljudge.common.security.SecurityUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantChat {

    private static final String REFUSED_REPLY =
            "Mình không thể hỗ trợ yêu cầu này. Bạn thử hỏi về SQL theo cách khác nhé.";

    private final ConversationStore conversationStore;
    private final AiQuotaService quotaService;
    private final AssistantModel assistantModel;
    private final ProblemServiceClient problemServiceClient;
    private final ContestLock contestLock;

    public ConversationSummaryResponse start(UUID problemId) {
        UUID userId = SecurityUtils.requireCurrentUser().id();
        contestLock.ensureNotInRatedContest(userId);
        ProblemRef problem = null;
        if (problemId != null) {
            ProblemContext context = problemServiceClient.assistantContext(problemId, userId);
            problem = new ProblemRef(context.problemId(), context.slug(), context.title());
        }
        return conversationStore.start(userId, problem);
    }

    public MessageExchangeResponse send(UUID conversationId, SendMessageRequest request) {
        UUID userId = SecurityUtils.requireCurrentUser().id();
        contestLock.ensureNotInRatedContest(userId);
        ConversationSnapshot snapshot = conversationStore.snapshot(conversationId, userId);
        ProblemContext problem =
                snapshot.problemId() == null
                        ? null
                        : problemServiceClient.assistantContext(snapshot.problemId(), userId);
        String question = AssistantPrompts.userTurn(request.content(), request.code());
        quotaService.consume(userId);
        AssistantReply reply = askModel(userId, snapshot, problem, question);
        boolean revealsSolution =
                problem != null
                        && !problem.isSolved()
                        && SolutionGuard.revealsSolution(reply.text());
        BlockReason blockReason =
                reply.isRefused()
                        ? BlockReason.MODEL_REFUSAL
                        : revealsSolution ? BlockReason.SOLUTION_REVEAL : null;
        String answer =
                blockReason == BlockReason.MODEL_REFUSAL
                        ? REFUSED_REPLY
                        : blockReason == BlockReason.SOLUTION_REVEAL
                                ? SolutionGuard.BLOCKED_REPLY
                                : reply.text();
        Exchange exchange =
                conversationStore.append(conversationId, userId, question, answer, blockReason);
        log.info(
                "ai_message_answered conversationId={} blockReason={}",
                conversationId,
                blockReason);
        return new MessageExchangeResponse(
                exchange.question(), exchange.answer(), quotaService.usage(userId));
    }

    private AssistantReply askModel(
            UUID userId, ConversationSnapshot snapshot, ProblemContext problem, String question) {
        List<ChatTurn> turns = new ArrayList<>(snapshot.history());
        turns.add(new ChatTurn(MessageRole.USER, question));
        try {
            return assistantModel.reply(AssistantPrompts.systemPrompt(problem), turns);
        } catch (RuntimeException error) {
            quotaService.refund(userId);
            throw error;
        }
    }
}
