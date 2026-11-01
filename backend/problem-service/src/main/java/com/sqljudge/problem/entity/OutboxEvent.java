package com.sqljudge.problem.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "outbox_events",
        indexes =
                @Index(
                        name = "idx_outbox_events_published_at_created_at",
                        columnList = "published_at, created_at"))
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent extends BaseEntity {

    @Column(name = "topic", nullable = false, length = 200)
    private String topic;

    @Column(name = "message_key", nullable = false, length = 100)
    private String messageKey;

    @Column(name = "payload", nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    public static OutboxEvent of(String topic, String messageKey, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.setTopic(topic);
        event.setMessageKey(messageKey);
        event.setPayload(payload);
        return event;
    }
}
