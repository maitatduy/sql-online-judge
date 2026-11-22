package com.sqljudge.contest.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.contest.client.ProblemServiceClient;
import com.sqljudge.contest.dto.response.ContestProblemStatementResponse;
import com.sqljudge.contest.dto.response.ContestantProblemResponse;
import com.sqljudge.contest.entity.Contest;
import com.sqljudge.contest.entity.ContestProblem;
import com.sqljudge.contest.repository.ContestRepository;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class ContestantProblemService {

    private final ContestRepository contestRepository;
    private final ContestAccess contestAccess;
    private final ProblemServiceClient problemServiceClient;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public List<ContestantProblemResponse> list(String slug) {
        return transactionTemplate.execute(
                status ->
                        accessibleContest(slug).getProblems().stream()
                                .map(ContestantProblemResponse::from)
                                .toList());
    }

    public ContestProblemStatementResponse statement(String slug, String letter) {
        ContestProblem problem =
                transactionTemplate.execute(
                        status ->
                                accessibleContest(slug).getProblems().stream()
                                        .filter(candidate -> candidate.letter().equals(letter))
                                        .findFirst()
                                        .orElseThrow(
                                                () ->
                                                        new NotFoundException(
                                                                ErrorCode.PROBLEM_NOT_FOUND,
                                                                "Contest không có bài này")));
        return new ContestProblemStatementResponse(
                problem.letter(), problemServiceClient.contestStatement(problem.getProblemId()));
    }

    private Contest accessibleContest(String slug) {
        Contest contest =
                contestRepository
                        .findBySlug(slug)
                        .orElseThrow(ContestQueryService::contestNotFound);
        contestAccess.requireProblemAccess(
                contest, SecurityUtils.requireCurrentUser(), clock.instant());
        return contest;
    }
}
