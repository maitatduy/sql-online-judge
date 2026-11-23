package com.sqljudge.contest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "sqljudge.contest.lifecycle-enabled", havingValue = "true")
@RequiredArgsConstructor
class ContestLifecycleScheduler {

    private final ContestLifecycleService lifecycleService;
    private final ContestFinalizer contestFinalizer;
    private final ContestRatingService contestRatingService;

    @Scheduled(fixedDelayString = "${sqljudge.contest.lifecycle-interval}")
    void announceDue() {
        lifecycleService.announceDue();
        contestFinalizer.finalizeDue();
        contestRatingService.applyDue();
    }
}
