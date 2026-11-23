package com.sqljudge.problem.service;

import com.sqljudge.problem.client.JudgingSummary;
import com.sqljudge.problem.client.JudgingSummary.UserJudging;
import com.sqljudge.problem.entity.ProblemStats;
import com.sqljudge.problem.entity.ProcessedEvent;
import com.sqljudge.problem.entity.UserProblemProgress;
import com.sqljudge.problem.event.ProblemTopics;
import com.sqljudge.problem.event.RejudgeFinishedEvent;
import com.sqljudge.problem.repository.ProblemRepository;
import com.sqljudge.problem.repository.ProblemStatsRepository;
import com.sqljudge.problem.repository.ProcessedEventRepository;
import com.sqljudge.problem.repository.UserProblemProgressRepository;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemStatsRecomputeService {

    private final ProcessedEventRepository processedEventRepository;
    private final ProblemRepository problemRepository;
    private final ProblemStatsRepository problemStatsRepository;
    private final UserProblemProgressRepository progressRepository;
    private final ProblemDetailCache problemDetailCache;

    @Transactional(readOnly = true)
    public boolean isProcessed(UUID eventId) {
        return processedEventRepository.existsByEventId(eventId);
    }

    @Transactional
    public void replace(RejudgeFinishedEvent event, JudgingSummary summary) {
        if (processedEventRepository.existsByEventId(event.eventId())) {
            return;
        }
        processedEventRepository.saveAndFlush(
                new ProcessedEvent(event.eventId(), ProblemTopics.REJUDGE_FINISHED));
        UUID problemId = event.problemId();
        ProblemStats stats =
                problemStatsRepository
                        .findByProblemId(problemId)
                        .orElseGet(() -> new ProblemStats(problemId, 0, 0, 0));
        stats.replace(
                summary.submissionCount(), summary.acceptedCount(), summary.solvedUserCount());
        problemStatsRepository.save(stats);
        replaceProgress(problemId, summary);
        evictAfterCommit(problemId);
        log.info(
                "problem_stats_recomputed problemId={} submissions={} accepted={}",
                problemId,
                summary.submissionCount(),
                summary.acceptedCount());
    }

    private void replaceProgress(UUID problemId, JudgingSummary summary) {
        Map<UUID, UserProblemProgress> existing =
                progressRepository.findAllByProblemId(problemId).stream()
                        .collect(
                                Collectors.toMap(
                                        UserProblemProgress::getUserId, Function.identity()));
        for (UserJudging user : summary.users()) {
            UserProblemProgress progress =
                    existing.getOrDefault(
                            user.userId(), UserProblemProgress.start(user.userId(), problemId));
            progress.resetTo(user.firstAcceptedAt());
            progressRepository.save(progress);
        }
    }

    private void evictAfterCommit(UUID problemId) {
        problemRepository
                .findById(problemId)
                .ifPresent(
                        problem -> {
                            String slug = problem.getSlug();
                            TransactionSynchronizationManager.registerSynchronization(
                                    new TransactionSynchronization() {
                                        @Override
                                        public void afterCommit() {
                                            problemDetailCache.evict(slug, problemId);
                                        }
                                    });
                        });
    }
}
