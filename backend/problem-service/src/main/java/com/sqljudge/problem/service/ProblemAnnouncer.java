package com.sqljudge.problem.service;

import com.sqljudge.common.outbox.OutboxWriter;
import com.sqljudge.common.persistence.UuidV7;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.event.ProblemPublishedEvent;
import com.sqljudge.problem.event.ProblemTopics;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
class ProblemAnnouncer {

    private static final int EVENT_VERSION = 1;

    private final OutboxWriter outboxWriter;
    private final ProblemDetailCache problemDetailCache;

    void announcePublic(Problem problem, Instant now) {
        outboxWriter.append(
                ProblemTopics.PROBLEM_PUBLISHED,
                problem.getId().toString(),
                new ProblemPublishedEvent(
                        UuidV7.generate(),
                        now,
                        EVENT_VERSION,
                        problem.getId(),
                        problem.getNumber(),
                        problem.getSlug(),
                        problem.getTitle(),
                        problem.getDifficulty(),
                        problem.getAuthorId()));
        String slug = problem.getSlug();
        UUID problemId = problem.getId();
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        problemDetailCache.evict(slug, problemId);
                    }
                });
        log.info("problem_published problemId={} number={}", problem.getId(), problem.getNumber());
    }
}
