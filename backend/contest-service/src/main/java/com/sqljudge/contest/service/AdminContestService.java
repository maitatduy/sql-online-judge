package com.sqljudge.contest.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.common.web.PageResponse;
import com.sqljudge.contest.dto.request.CreateContestRequest;
import com.sqljudge.contest.dto.response.AdminContestResponse;
import com.sqljudge.contest.dto.response.ContestResponse;
import com.sqljudge.contest.dto.response.ContestSummaryResponse;
import com.sqljudge.contest.entity.Contest;
import com.sqljudge.contest.repository.ContestRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminContestService {

    private final ContestRepository contestRepository;
    private final Clock clock;

    @Transactional
    public ContestResponse create(CreateContestRequest request) {
        Instant now = clock.instant();
        if (!request.startAt().isAfter(now)) {
            throw new InvalidFieldException("startAt", "Giờ bắt đầu phải ở tương lai");
        }
        if (request.freezeMinutes() >= request.durationMinutes()) {
            throw new InvalidFieldException(
                    "freezeMinutes", "Thời gian đóng băng phải ngắn hơn thời lượng contest");
        }
        if (contestRepository.existsBySlug(request.slug())) {
            throw new ConflictException(ErrorCode.SLUG_TAKEN, "Slug đã được contest khác dùng");
        }
        Contest contest = contestRepository.save(Contest.schedule(request.toSchedule()));
        return ContestResponse.from(contest, now);
    }

    @Transactional(readOnly = true)
    public PageResponse<ContestSummaryResponse> list(int page, int size) {
        Instant now = clock.instant();
        return PageResponse.from(
                contestRepository.findAllByOrderByStartAtDesc(PageRequest.of(page - 1, size)),
                contest -> ContestSummaryResponse.from(contest, now));
    }

    @Transactional(readOnly = true)
    public AdminContestResponse get(UUID contestId) {
        Contest contest =
                contestRepository
                        .findById(contestId)
                        .orElseThrow(ContestQueryService::contestNotFound);
        return AdminContestResponse.from(contest, clock.instant());
    }
}
