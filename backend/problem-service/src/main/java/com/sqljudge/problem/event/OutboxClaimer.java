package com.sqljudge.problem.event;

import com.sqljudge.problem.entity.OutboxEvent;
import com.sqljudge.problem.repository.OutboxEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OutboxClaimer {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxProperties properties;
    private final Clock clock;

    @Transactional
    public List<OutboxMessage> claim(int limit) {
        Instant now = clock.instant();
        List<OutboxEvent> events =
                outboxEventRepository.findClaimable(
                        now.minus(properties.claimTimeout()), PageRequest.of(0, limit));
        events.forEach(event -> event.setClaimedAt(now));
        return events.stream().map(OutboxMessage::from).toList();
    }

    @Transactional
    public void markPublished(List<UUID> ids) {
        outboxEventRepository.markPublished(ids, clock.instant());
    }

    @Transactional
    public void release(List<UUID> ids) {
        outboxEventRepository.release(ids);
    }
}
