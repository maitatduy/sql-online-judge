package com.sqljudge.contest.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.contest.client.ProblemServiceClient;
import com.sqljudge.contest.dto.request.ReplaceContestProblemsRequest;
import com.sqljudge.contest.dto.response.AdminContestResponse;
import com.sqljudge.contest.entity.Contest;
import com.sqljudge.contest.entity.ContestProblemInfo;
import com.sqljudge.contest.repository.ContestRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContestProblemService {

    private final ContestRepository contestRepository;
    private final ProblemServiceClient problemServiceClient;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public AdminContestResponse replace(UUID contestId, ReplaceContestProblemsRequest request) {
        if (request.hasDuplicates()) {
            throw new InvalidFieldException("problemIds", "Mỗi bài chỉ được gắn một lần");
        }
        transactionTemplate.executeWithoutResult(status -> editableContest(contestId));
        List<ContestProblemInfo> problems = readyProblems(request.problemIds());
        return transactionTemplate.execute(
                status -> {
                    Contest contest = editableContest(contestId);
                    contest.clearProblems();
                    contestRepository.saveAndFlush(contest);
                    contest.addProblems(problems);
                    Contest saved = contestRepository.saveAndFlush(contest);
                    log.info(
                            "contest_problems_replaced contestId={} count={}",
                            contestId,
                            problems.size());
                    return AdminContestResponse.from(saved, clock.instant());
                });
    }

    private List<ContestProblemInfo> readyProblems(List<UUID> problemIds) {
        if (problemIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, ContestProblemInfo> ready =
                problemServiceClient.contestReady(problemIds).stream()
                        .collect(
                                Collectors.toMap(
                                        ContestProblemInfo::problemId, Function.identity()));
        if (!ready.keySet().containsAll(problemIds)) {
            throw new BusinessRuleException(
                    ErrorCode.CONTEST_PROBLEM_NOT_READY,
                    "Có bài chưa được duyệt cho contest hoặc không còn tồn tại");
        }
        return problemIds.stream().map(ready::get).toList();
    }

    private Contest editableContest(UUID contestId) {
        Contest contest =
                contestRepository
                        .findById(contestId)
                        .orElseThrow(ContestQueryService::contestNotFound);
        Instant now = clock.instant();
        if (contest.hasStartedAt(now)) {
            throw new ConflictException(
                    ErrorCode.CONTEST_ALREADY_STARTED,
                    "Contest đã bắt đầu, không đổi bài được nữa");
        }
        return contest;
    }
}
