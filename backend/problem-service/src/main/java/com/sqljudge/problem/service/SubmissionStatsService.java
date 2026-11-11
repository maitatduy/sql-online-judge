package com.sqljudge.problem.service;

import com.sqljudge.problem.entity.ProblemStats;
import com.sqljudge.problem.entity.ProcessedEvent;
import com.sqljudge.problem.entity.UserProblemProgress;
import com.sqljudge.problem.event.ProblemTopics;
import com.sqljudge.problem.event.SubmissionJudgedEvent;
import com.sqljudge.problem.repository.ProblemRepository;
import com.sqljudge.problem.repository.ProblemStatsRepository;
import com.sqljudge.problem.repository.ProcessedEventRepository;
import com.sqljudge.problem.repository.UserProblemProgressRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionStatsService {

    private final ProcessedEventRepository processedEventRepository;
    private final ProblemRepository problemRepository;
    private final ProblemStatsRepository problemStatsRepository;
    private final UserProblemProgressRepository userProblemProgressRepository;
    private final Clock clock;

    @Transactional
    public void record(SubmissionJudgedEvent event) {
        if (processedEventRepository.existsByEventId(event.eventId())) {
            return;
        }
        processedEventRepository.saveAndFlush(
                new ProcessedEvent(event.eventId(), ProblemTopics.SUBMISSION_JUDGED));
        if (event.isSystemError()) {
            return;
        }
        if (!problemRepository.existsById(event.problemId())) {
            log.warn(
                    "submission_judged_unknown_problem submissionId={} problemId={}",
                    event.submissionId(),
                    event.problemId());
            return;
        }
        UserProblemProgress progress =
                userProblemProgressRepository
                        .findByUserIdAndProblemId(event.userId(), event.problemId())
                        .orElseGet(
                                () -> UserProblemProgress.start(event.userId(), event.problemId()));
        boolean newlySolved = progress.recordJudgement(event.isAccepted(), event.judgedAt());
        userProblemProgressRepository.saveAndFlush(progress);
        recordStats(event, newlySolved);
    }

    private void recordStats(SubmissionJudgedEvent event, boolean newlySolved) {
        long accepted = event.isAccepted() ? 1 : 0;
        long solved = newlySolved ? 1 : 0;
        int updated =
                problemStatsRepository.recordSubmission(
                        event.problemId(), accepted, solved, clock.instant());
        if (updated == 0) {
            problemStatsRepository.saveAndFlush(
                    new ProblemStats(event.problemId(), 1, accepted, solved));
        }
    }
}
