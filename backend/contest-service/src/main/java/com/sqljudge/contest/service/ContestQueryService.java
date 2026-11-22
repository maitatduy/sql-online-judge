package com.sqljudge.contest.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.web.PageResponse;
import com.sqljudge.contest.dto.response.ContestResponse;
import com.sqljudge.contest.dto.response.ContestSummaryResponse;
import com.sqljudge.contest.dto.response.PublicContestResponse;
import com.sqljudge.contest.entity.Contest;
import com.sqljudge.contest.entity.ContestPhase;
import com.sqljudge.contest.repository.ContestRegistrationRepository;
import com.sqljudge.contest.repository.ContestRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContestQueryService {

    private final ContestRepository contestRepository;
    private final ContestRegistrationRepository registrationRepository;
    private final Clock clock;

    static NotFoundException contestNotFound() {
        return new NotFoundException(ErrorCode.CONTEST_NOT_FOUND, "Không tìm thấy contest");
    }

    @Transactional(readOnly = true)
    public PageResponse<ContestSummaryResponse> list(ContestPhase phase, int page, int size) {
        Instant now = clock.instant();
        return PageResponse.from(
                findByPhase(phase, now, PageRequest.of(page - 1, size)),
                contest -> ContestSummaryResponse.from(contest, now));
    }

    @Transactional(readOnly = true)
    public PublicContestResponse getBySlug(String slug) {
        Contest contest =
                contestRepository
                        .findBySlug(slug)
                        .orElseThrow(ContestQueryService::contestNotFound);
        return new PublicContestResponse(
                ContestResponse.from(contest, clock.instant()),
                registrationRepository.countByContestId(contest.getId()));
    }

    private Page<Contest> findByPhase(ContestPhase phase, Instant now, Pageable pageable) {
        return switch (phase) {
            case UPCOMING -> contestRepository.findByStartAtAfterOrderByStartAtAsc(now, pageable);
            case RUNNING -> contestRepository.findRunningAt(now, pageable);
            case FINISHED ->
                    contestRepository.findByEndAtLessThanEqualOrderByEndAtDesc(now, pageable);
        };
    }
}
