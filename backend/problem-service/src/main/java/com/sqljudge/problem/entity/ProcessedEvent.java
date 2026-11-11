package com.sqljudge.problem.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "processed_events",
        indexes =
                @Index(
                        name = "uk_processed_events_event_id",
                        columnList = "event_id",
                        unique = true))
@Getter
@NoArgsConstructor
public class ProcessedEvent extends BaseEntity {

    @Column(name = "event_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID eventId;

    @Column(name = "topic", nullable = false, length = 100)
    private String topic;

    public ProcessedEvent(UUID eventId, String topic) {
        this.eventId = eventId;
        this.topic = topic;
    }
}
