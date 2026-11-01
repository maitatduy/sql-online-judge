package com.sqljudge.problem.event;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sqljudge.outbox", name = "relay-enabled", havingValue = "true")
public class OutboxRelay {

    private final OutboxClaimer claimer;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxProperties properties;

    @Scheduled(fixedDelayString = "${sqljudge.outbox.relay-interval}")
    public void relay() {
        List<OutboxMessage> claimed = claimer.claim(properties.batchSize());
        if (claimed.isEmpty()) {
            return;
        }
        int sent = 0;
        while (sent < claimed.size() && send(claimed.get(sent))) {
            sent++;
        }
        if (sent > 0) {
            claimer.markPublished(idsOf(claimed.subList(0, sent)));
        }
        if (sent < claimed.size()) {
            claimer.release(idsOf(claimed.subList(sent, claimed.size())));
        }
    }

    private boolean send(OutboxMessage message) {
        try {
            kafkaTemplate
                    .send(message.topic(), message.key(), message.payload())
                    .get(properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
            return true;
        } catch (ExecutionException | TimeoutException exception) {
            log.warn(
                    "outbox_publish_failed topic={} key={}",
                    message.topic(),
                    message.key(),
                    exception);
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static List<UUID> idsOf(List<OutboxMessage> messages) {
        return messages.stream().map(OutboxMessage::id).toList();
    }
}
