package com.sqljudge.judge.sandbox;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

public class OrphanSandboxSweeper {

    private static final Logger log = LoggerFactory.getLogger(OrphanSandboxSweeper.class);

    private final List<OrphanCleaner> cleaners;
    private final Clock clock;
    private final Duration orphanAge;

    public OrphanSandboxSweeper(List<OrphanCleaner> cleaners, Clock clock, Duration orphanAge) {
        this.cleaners = List.copyOf(cleaners);
        this.clock = clock;
        this.orphanAge = orphanAge;
    }

    @Scheduled(
            initialDelayString = "${sqljudge.sandbox.sweep-interval}",
            fixedDelayString = "${sqljudge.sandbox.sweep-interval}")
    public void sweep() {
        Instant cutoff = clock.instant().minus(orphanAge);
        for (OrphanCleaner cleaner : cleaners) {
            String sandbox = cleaner.getClass().getSimpleName();
            try {
                int removed = cleaner.removeRunsCreatedBefore(cutoff);
                if (removed > 0) {
                    log.info("sandbox_orphans_removed sandbox={} count={}", sandbox, removed);
                }
            } catch (RuntimeException exception) {
                log.warn("sandbox_orphan_sweep_failed sandbox={}", sandbox, exception);
            }
        }
    }
}
