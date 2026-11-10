package com.sqljudge.common.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sqljudge.common.outbox.OutboxClaimer;
import com.sqljudge.common.outbox.OutboxMessage;
import com.sqljudge.common.outbox.OutboxProperties;
import com.sqljudge.common.outbox.OutboxRelay;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

class OutboxRelayTest {

    private static final OutboxProperties PROPERTIES =
            new OutboxProperties(
                    Duration.ofMillis(500), 100, Duration.ofSeconds(1), Duration.ofSeconds(30));

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);

    private final OutboxClaimer claimer = mock(OutboxClaimer.class);
    private final OutboxRelay relay = new OutboxRelay(claimer, kafkaTemplate, PROPERTIES);

    @Test
    @DisplayName("Gửi lỗi giữa chừng thì dừng lô, đánh dấu phần đã gửi và nhả phần còn lại")
    void relay_sendFails_stopsBatchAndReleasesRest() {
        OutboxMessage first = message("first");
        OutboxMessage second = message("second");
        OutboxMessage third = message("third");
        when(claimer.claim(100)).thenReturn(List.of(first, second, third));
        when(kafkaTemplate.send("topic", "first", "first"))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        when(kafkaTemplate.send("topic", "second", "second"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("down")));

        relay.relay();

        verify(claimer).markPublished(List.of(first.id()));
        verify(claimer).release(List.of(second.id(), third.id()));
        verify(kafkaTemplate, never()).send("topic", "third", "third");
    }

    @Test
    @DisplayName("Không có event nào thì không gửi gì")
    void relay_nothingClaimed_sendsNothing() {
        when(claimer.claim(100)).thenReturn(List.of());

        relay.relay();

        verify(kafkaTemplate, never())
                .send(any(String.class), any(String.class), any(String.class));
    }

    private static OutboxMessage message(String value) {
        return new OutboxMessage(UUID.randomUUID(), "topic", value, value);
    }
}
