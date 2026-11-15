package com.sqljudge.submission.service;

import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.submission.dto.response.ActivityDayResponse;
import com.sqljudge.submission.dto.response.RecentSubmissionResponse;
import com.sqljudge.submission.dto.response.SubmissionStatsResponse;
import com.sqljudge.submission.repository.FirstSolveRow;
import com.sqljudge.submission.repository.SubmissionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmissionStatsService {

    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Duration WEEK = Duration.ofDays(7);
    private static final int DEFAULT_ACTIVITY_DAYS = 364;
    private static final int MAX_ACTIVITY_DAYS = 366;

    private final SubmissionRepository submissionRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public SubmissionStatsResponse summary() {
        UUID userId = SecurityUtils.requireCurrentUser().id();
        List<FirstSolveRow> firstSolves = submissionRepository.findFirstSolves(userId);
        Instant weekAgo = clock.instant().minus(WEEK);
        long solvedThisWeek =
                firstSolves.stream()
                        .filter(solve -> !solve.firstSolvedAt().isBefore(weekAgo))
                        .count();
        return new SubmissionStatsResponse(
                submissionRepository.countOwn(userId),
                submissionRepository.countOwnAccepted(userId),
                firstSolves.size(),
                solvedThisWeek);
    }

    @Transactional(readOnly = true)
    public List<ActivityDayResponse> activity(LocalDate from) {
        LocalDate today = LocalDate.now(clock.withZone(VIETNAM));
        LocalDate start = from == null ? today.minusDays(DEFAULT_ACTIVITY_DAYS) : from;
        if (start.isBefore(today.minusDays(MAX_ACTIVITY_DAYS))) {
            throw new InvalidFieldException(
                    "from", "Chỉ xem được hoạt động trong một năm gần nhất");
        }
        UUID userId = SecurityUtils.requireCurrentUser().id();
        Map<LocalDate, Long> counts =
                submissionRepository
                        .findCreatedAtSince(userId, start.atStartOfDay(VIETNAM).toInstant())
                        .stream()
                        .map(createdAt -> LocalDate.ofInstant(createdAt, VIETNAM))
                        .collect(
                                Collectors.groupingBy(
                                        Function.identity(), TreeMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .map(day -> new ActivityDayResponse(day.getKey(), day.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecentSubmissionResponse> recent(int size) {
        UUID userId = SecurityUtils.requireCurrentUser().id();
        return submissionRepository.findRecent(userId, Limit.of(size));
    }
}
