package com.sqljudge.contest.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.contest.dto.response.StandingsResponse;
import com.sqljudge.contest.dto.response.StandingsResponse.MyStanding;
import com.sqljudge.contest.dto.response.StandingsResponse.StandingCellResponse;
import com.sqljudge.contest.dto.response.StandingsResponse.StandingItem;
import com.sqljudge.contest.entity.Contest;
import com.sqljudge.contest.entity.ContestProblem;
import com.sqljudge.contest.repository.ContestCellRepository;
import com.sqljudge.contest.repository.ContestRegistrationRepository;
import com.sqljudge.contest.repository.ContestRepository;
import com.sqljudge.contest.standings.StandingRow;
import com.sqljudge.contest.standings.StandingRows;
import com.sqljudge.contest.standings.StandingsStore;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class StandingsService {

    private final ContestRepository contestRepository;
    private final ContestCellRepository cellRepository;
    private final ContestRegistrationRepository registrationRepository;
    private final StandingsStore standingsStore;
    private final Clock clock;

    void refreshAfterCommit(Contest contest, UUID userId) {
        StandingRow row = publicRowOf(contest, userId);
        if (row == null) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            standingsStore.put(contest.getId(), contest.getEndAt(), row);
                        } catch (DataAccessException failure) {
                            log.warn(
                                    "standings_cache_update_failed contestId={}",
                                    contest.getId(),
                                    failure);
                            standingsStore.discardQuietly(contest.getId());
                        }
                    }
                });
    }

    @Transactional(readOnly = true)
    public UUID contestIdOf(String slug) {
        return contestBySlug(slug).getId();
    }

    @Transactional(readOnly = true)
    public StandingsResponse standings(String slug, int page, int size) {
        Contest contest = contestBySlug(slug);
        if (standingsStore.size(contest.getId()) == 0) {
            rebuild(contest);
        }
        long total = standingsStore.size(contest.getId());
        Map<UUID, String> letters = lettersOf(contest);
        List<StandingItem> items =
                standingsStore.page(contest.getId(), (long) (page - 1) * size, size).stream()
                        .map(
                                ranked ->
                                        new StandingItem(
                                                ranked.rank(),
                                                ranked.row().username(),
                                                ranked.row().displayName(),
                                                ranked.row().solved(),
                                                ranked.row().penaltyMinutes(),
                                                cellsOf(ranked.row(), letters)))
                        .toList();
        return new StandingsResponse(
                contest.getProblems().stream().map(ContestProblem::letter).toList(),
                !clock.instant().isBefore(contest.scoringRules().freezeAt()),
                items,
                page,
                size,
                total,
                (int) Math.ceil((double) total / size));
    }

    @Transactional(readOnly = true)
    public MyStanding mine(String slug) {
        Contest contest = contestBySlug(slug);
        UUID userId = SecurityUtils.requireCurrentUser().id();
        StandingRow row =
                registrationRepository
                        .findByContestIdAndUserId(contest.getId(), userId)
                        .map(
                                registration ->
                                        StandingRows.actualRow(
                                                contest,
                                                registration,
                                                cellRepository.findByContestIdAndUserId(
                                                        contest.getId(), userId)))
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.CONTEST_NOT_REGISTERED,
                                                "Bạn chưa đăng ký contest này"));
        return new MyStanding(row.solved(), row.penaltyMinutes(), cellsOf(row, lettersOf(contest)));
    }

    private void rebuild(Contest contest) {
        cellRepository.findUserIdsByContestId(contest.getId()).stream()
                .map(userId -> publicRowOf(contest, userId))
                .filter(row -> row != null)
                .forEach(row -> standingsStore.put(contest.getId(), contest.getEndAt(), row));
    }

    private StandingRow publicRowOf(Contest contest, UUID userId) {
        return registrationRepository
                .findByContestIdAndUserId(contest.getId(), userId)
                .map(
                        registration ->
                                StandingRows.publicRow(
                                        contest,
                                        registration,
                                        cellRepository.findByContestIdAndUserId(
                                                contest.getId(), userId)))
                .orElse(null);
    }

    private Contest contestBySlug(String slug) {
        return contestRepository.findBySlug(slug).orElseThrow(ContestQueryService::contestNotFound);
    }

    private static Map<UUID, String> lettersOf(Contest contest) {
        return contest.getProblems().stream()
                .collect(Collectors.toMap(ContestProblem::getProblemId, ContestProblem::letter));
    }

    private static List<StandingCellResponse> cellsOf(StandingRow row, Map<UUID, String> letters) {
        return row.cells().stream()
                .filter(cell -> letters.containsKey(cell.problemId()))
                .map(
                        cell ->
                                new StandingCellResponse(
                                        letters.get(cell.problemId()),
                                        cell.acceptedMinute(),
                                        cell.wrongAttempts(),
                                        cell.pendingAttempts()))
                .toList();
    }
}
