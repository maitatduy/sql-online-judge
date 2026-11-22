package com.sqljudge.contest.service;

import com.sqljudge.common.outbox.OutboxWriter;
import com.sqljudge.common.persistence.UuidV7;
import com.sqljudge.contest.entity.Contest;
import com.sqljudge.contest.entity.ContestProblem;
import com.sqljudge.contest.event.ContestFinishedEvent;
import com.sqljudge.contest.event.ContestStartedEvent;
import com.sqljudge.contest.event.ContestTopics;
import com.sqljudge.contest.repository.ContestRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContestLifecycleService {

    private static final int EVENT_VERSION = 1;

    private final ContestRepository contestRepository;
    private final OutboxWriter outboxWriter;
    private final Clock clock;

    @Transactional
    public void announceDue() {
        Instant now = clock.instant();
        for (Contest contest : contestRepository.findDueForAnnouncement(now)) {
            if (contest.needsStartAnnouncementAt(now)) {
                announceStart(contest, now);
            }
            if (contest.needsFinishAnnouncementAt(now)) {
                announceFinish(contest, now);
            }
        }
    }

    private void announceStart(Contest contest, Instant now) {
        outboxWriter.append(
                ContestTopics.CONTEST_STARTED,
                contest.getId().toString(),
                new ContestStartedEvent(
                        UuidV7.generate(),
                        now,
                        EVENT_VERSION,
                        contest.getId(),
                        contest.getStartAt(),
                        contest.getEndAt(),
                        contest.isRated()));
        contest.markStartAnnounced(now);
        log.info("contest_started contestId={}", contest.getId());
    }

    private void announceFinish(Contest contest, Instant now) {
        outboxWriter.append(
                ContestTopics.CONTEST_FINISHED,
                contest.getId().toString(),
                new ContestFinishedEvent(
                        UuidV7.generate(),
                        now,
                        EVENT_VERSION,
                        contest.getId(),
                        contest.getProblems().stream().map(ContestProblem::getProblemId).toList()));
        contest.markFinishAnnounced(now);
        log.info("contest_finished contestId={}", contest.getId());
    }
}
