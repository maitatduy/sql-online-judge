package com.sqljudge.ai.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "ai_messages",
        indexes = {
            @Index(
                    name = "idx_ai_messages_conversation_id_created_at",
                    columnList = "conversation_id, created_at"),
            @Index(
                    name = "idx_ai_messages_is_blocked_created_at",
                    columnList = "is_blocked, created_at"),
            @Index(name = "idx_ai_messages_role_created_at", columnList = "role, created_at")
        })
@Getter
@NoArgsConstructor
public class AiMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private AiConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    private MessageRole role;

    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(name = "is_blocked", nullable = false)
    private boolean blocked;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_reason", length = 20)
    private BlockReason blockReason;

    @Column(name = "reply_to_id", columnDefinition = "BINARY(16)")
    private UUID replyToId;

    public static AiMessage question(AiConversation conversation, String content) {
        AiMessage message = new AiMessage();
        message.conversation = conversation;
        message.role = MessageRole.USER;
        message.content = content;
        return message;
    }

    public static AiMessage answer(
            AiConversation conversation, AiMessage question, String content, BlockReason reason) {
        AiMessage message = new AiMessage();
        message.conversation = conversation;
        message.role = MessageRole.ASSISTANT;
        message.content = content;
        message.replyToId = question.getId();
        message.blocked = reason != null;
        message.blockReason = reason;
        return message;
    }
}
