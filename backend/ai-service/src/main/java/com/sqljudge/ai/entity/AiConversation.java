package com.sqljudge.ai.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "ai_conversations",
        indexes =
                @Index(
                        name = "idx_ai_conversations_user_id_last_message_at",
                        columnList = "user_id, last_message_at"))
@Getter
@NoArgsConstructor
public class AiConversation extends BaseEntity {

    private static final int MAX_TITLE_LENGTH = 200;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "problem_id", columnDefinition = "BINARY(16)")
    private UUID problemId;

    @Column(name = "problem_slug", length = 160)
    private String problemSlug;

    @Column(name = "title", nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt;

    public static AiConversation start(UUID userId, ProblemRef problem, Instant now) {
        AiConversation conversation = new AiConversation();
        conversation.userId = userId;
        conversation.title = problem == null ? "Hội thoại mới" : problem.title();
        if (problem != null) {
            conversation.problemId = problem.id();
            conversation.problemSlug = problem.slug();
        }
        conversation.lastMessageAt = now;
        return conversation;
    }

    public boolean isOwnedBy(UUID candidate) {
        return userId.equals(candidate);
    }

    public void messageAdded(String firstQuestion, boolean isFirstMessage, Instant now) {
        lastMessageAt = now;
        if (isFirstMessage && problemId == null) {
            String compact = firstQuestion.strip().replaceAll("\\s+", " ");
            title =
                    compact.length() > MAX_TITLE_LENGTH
                            ? compact.substring(0, MAX_TITLE_LENGTH)
                            : compact;
        }
    }

    public record ProblemRef(UUID id, String slug, String title) {}
}
