package com.sqljudge.contest.service;

import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.contest.dto.response.ContestSummaryResponse;
import com.sqljudge.contest.entity.Contest;
import com.sqljudge.contest.repository.ContestAttemptRepository;
import com.sqljudge.contest.repository.ContestCount;
import com.sqljudge.contest.repository.ContestRegistrationRepository;
import com.sqljudge.contest.repository.ContestRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ContestStatsReader {

    private final ContestRepository contestRepository;
    private final ContestRegistrationRepository registrationRepository;
    private final ContestAttemptRepository attemptRepository;

    ContestStats statsOf(List<Contest> contests) {
        List<UUID> ids = contests.stream().map(Contest::getId).toList();
        if (ids.isEmpty()) {
            return ContestStats.EMPTY;
        }
        Set<UUID> registered =
                SecurityUtils.currentUserId()
                        .<Set<UUID>>map(
                                userId ->
                                        new HashSet<>(
                                                registrationRepository.findRegisteredContestIds(
                                                        userId, ids)))
                        .orElse(Set.of());
        return new ContestStats(
                totals(contestRepository.countProblemsByContestIds(ids)),
                totals(registrationRepository.countByContestIds(ids)),
                totals(attemptRepository.countParticipantsByContestIds(ids)),
                registered);
    }

    private static Map<UUID, Long> totals(List<ContestCount> counts) {
        return counts.stream()
                .collect(Collectors.toMap(ContestCount::contestId, ContestCount::total));
    }

    record ContestStats(
            Map<UUID, Long> problems,
            Map<UUID, Long> registrants,
            Map<UUID, Long> participants,
            Set<UUID> registered) {

        static final ContestStats EMPTY = new ContestStats(Map.of(), Map.of(), Map.of(), Set.of());

        ContestSummaryResponse summaryOf(Contest contest, Instant now) {
            UUID id = contest.getId();
            return new ContestSummaryResponse(
                    id,
                    contest.getSlug(),
                    contest.getTitle(),
                    contest.getStartAt(),
                    contest.getEndAt(),
                    contest.getDurationMinutes(),
                    contest.isRated(),
                    contest.phaseAt(now),
                    problems.getOrDefault(id, 0L),
                    registrants.getOrDefault(id, 0L),
                    participants.getOrDefault(id, 0L),
                    registered.contains(id));
        }
    }
}
