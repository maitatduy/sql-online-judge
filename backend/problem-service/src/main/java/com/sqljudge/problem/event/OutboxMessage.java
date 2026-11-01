package com.sqljudge.problem.event;

import com.sqljudge.problem.entity.OutboxEvent;
import java.util.UUID;

public record OutboxMessage(UUID id, String topic, String key, String payload) {

    static OutboxMessage from(OutboxEvent event) {
        return new OutboxMessage(
                event.getId(), event.getTopic(), event.getMessageKey(), event.getPayload());
    }
}
