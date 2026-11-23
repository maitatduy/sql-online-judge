package com.sqljudge.contest.service;

import com.sqljudge.common.exception.AppException;
import com.sqljudge.contest.client.ProblemServiceClient;
import com.sqljudge.contest.repository.ContestRepository;
import com.sqljudge.contest.repository.StatementWarmupTarget;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContestStatementWarmer {

    private static final Duration WARMUP_LEAD = Duration.ofMinutes(1);

    private final ContestRepository contestRepository;
    private final ProblemServiceClient problemServiceClient;
    private final ContestLifecycleService lifecycleService;
    private final Clock clock;

    public void warmDue() {
        Instant now = clock.instant();
        Map<UUID, List<UUID>> problemIdsByContest =
                contestRepository.findDueForStatementWarmup(now, now.plus(WARMUP_LEAD)).stream()
                        .collect(
                                Collectors.groupingBy(
                                        StatementWarmupTarget::contestId,
                                        LinkedHashMap::new,
                                        Collectors.mapping(
                                                StatementWarmupTarget::problemId,
                                                Collectors.toList())));
        problemIdsByContest.forEach(
                (contestId, problemIds) -> {
                    long fetched = problemIds.stream().filter(this::fetchStatement).count();
                    if (fetched == problemIds.size()) {
                        lifecycleService.markStatementsWarmed(contestId, now);
                    }
                });
    }

    private boolean fetchStatement(UUID problemId) {
        try {
            problemServiceClient.contestStatement(problemId);
            return true;
        } catch (AppException failure) {
            log.warn("contest_statement_warmup_failed problemId={}", problemId);
            return false;
        }
    }
}
