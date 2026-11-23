package com.sqljudge.contest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class StandingsPushScheduler {

    private final StandingsStreamService streamService;

    @Scheduled(fixedDelayString = "${sqljudge.contest.standings-push-interval}")
    void push() {
        streamService.flush();
    }
}
