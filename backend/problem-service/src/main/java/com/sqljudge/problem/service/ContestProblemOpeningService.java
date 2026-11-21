package com.sqljudge.problem.service;

import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import com.sqljudge.problem.entity.ProcessedEvent;
import com.sqljudge.problem.event.ContestFinishedEvent;
import com.sqljudge.problem.event.ProblemTopics;
import com.sqljudge.problem.repository.ProblemRepository;
import com.sqljudge.problem.repository.ProcessedEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContestProblemOpeningService {

    private final ProcessedEventRepository processedEventRepository;
    private final ProblemRepository problemRepository;
    private final ProblemAnnouncer problemAnnouncer;
    private final Clock clock;

    @Transactional
    public void open(ContestFinishedEvent event) {
        if (processedEventRepository.existsByEventId(event.eventId())) {
            return;
        }
        processedEventRepository.saveAndFlush(
                new ProcessedEvent(event.eventId(), ProblemTopics.CONTEST_FINISHED));
        List<Problem> problems =
                problemRepository
                        .findByIdInAndStatusAndVisibilityAndDeletedAtIsNull(
                                event.problemIds(),
                                ProblemStatus.PUBLISHED,
                                ProblemVisibility.CONTEST_ONLY)
                        .stream()
                        .sorted(Comparator.comparing(Problem::getPublishedAt))
                        .toList();
        Instant now = clock.instant();
        int nextNumber = problemRepository.findMaxNumber() + 1;
        for (Problem problem : problems) {
            problem.openToPublic(nextNumber++);
            problemAnnouncer.announcePublic(problemRepository.saveAndFlush(problem), now);
        }
        log.info(
                "contest_problems_opened contestId={} count={}",
                event.contestId(),
                problems.size());
    }
}
