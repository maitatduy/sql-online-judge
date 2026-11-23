package com.sqljudge.contest.service;

import com.sqljudge.contest.dto.response.ProblemUsageResponse;
import com.sqljudge.contest.repository.ContestRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContestProblemUsageService {

    private final ContestRepository contestRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ProblemUsageResponse usageOf(UUID problemId) {
        return new ProblemUsageResponse(
                contestRepository.isProblemInUnfinishedContest(problemId, clock.instant()));
    }
}
