package com.sqljudge.contest.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.contest.dto.response.ContestEntryResponse;
import com.sqljudge.contest.entity.Contest;
import com.sqljudge.contest.entity.ContestProblem;
import com.sqljudge.contest.repository.ContestRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContestEntryService {

    private final ContestRepository contestRepository;
    private final ContestAccess contestAccess;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ContestEntryResponse entry(String slug, String letter, UUID userId) {
        Contest contest =
                contestRepository
                        .findBySlug(slug)
                        .orElseThrow(ContestQueryService::contestNotFound);
        ContestProblem problem =
                contest.problemByLetter(letter)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.PROBLEM_NOT_FOUND,
                                                "Contest không có bài này"));
        contestAccess.requireSubmissionEntry(contest, userId, clock.instant());
        return new ContestEntryResponse(contest.getId(), problem.getProblemId(), problem.letter());
    }
}
