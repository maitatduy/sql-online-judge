package com.sqljudge.common.outbox;

import java.util.UUID;

public record OutboxMessage(UUID id, String topic, String key, String payload) {

    static OutboxMessage from(OutboxEvent event) {
        return new OutboxMessage(
                event.getId(), event.getTopic(), event.getMessageKey(), event.getPayload());
    }
}
