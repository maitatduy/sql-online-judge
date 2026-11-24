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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "ai_messages",
        indexes =
                @Index(
                        name = "idx_ai_messages_conversation_id_created_at",
                        columnList = "conversation_id, created_at"))
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

    public static AiMessage of(
            AiConversation conversation, MessageRole role, String content, boolean blocked) {
        AiMessage message = new AiMessage();
        message.conversation = conversation;
        message.role = role;
        message.content = content;
        message.blocked = blocked;
        return message;
    }
}
