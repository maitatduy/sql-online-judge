package com.sqljudge.contest.service;

import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.contest.dto.response.ContestantProblemResponse;
import com.sqljudge.contest.entity.Contest;
import com.sqljudge.contest.repository.ContestRepository;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContestantProblemService {

    private final ContestRepository contestRepository;
    private final ContestAccess contestAccess;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<ContestantProblemResponse> list(String slug) {
        Contest contest =
                contestRepository
                        .findBySlug(slug)
                        .orElseThrow(ContestQueryService::contestNotFound);
        contestAccess.requireProblemAccess(
                contest, SecurityUtils.requireCurrentUser(), clock.instant());
        return contest.getProblems().stream().map(ContestantProblemResponse::from).toList();
    }
}
