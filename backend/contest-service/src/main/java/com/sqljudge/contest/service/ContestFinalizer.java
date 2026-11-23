package com.sqljudge.contest.service;

import com.sqljudge.contest.entity.Contest;
import com.sqljudge.contest.entity.ContestRegistration;
import com.sqljudge.contest.entity.ContestStanding;
import com.sqljudge.contest.repository.ContestCellRepository;
import com.sqljudge.contest.repository.ContestRegistrationRepository;
import com.sqljudge.contest.repository.ContestRepository;
import com.sqljudge.contest.repository.ContestStandingRepository;
import com.sqljudge.contest.scoring.FinalRanks;
import com.sqljudge.contest.scoring.FinalRanks.Ranked;
import com.sqljudge.contest.scoring.FinalRanks.Result;
import com.sqljudge.contest.standings.StandingRow;
import com.sqljudge.contest.standings.StandingRows;
import com.sqljudge.contest.standings.StandingsStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
public class ContestFinalizer {

    private final ContestRepository contestRepository;
    private final ContestCellRepository cellRepository;
    private final ContestRegistrationRepository registrationRepository;
    private final ContestStandingRepository standingRepository;
    private final StandingsStore standingsStore;
    private final Clock clock;
    private final Duration grace;

    public ContestFinalizer(
            ContestRepository contestRepository,
            ContestCellRepository cellRepository,
            ContestRegistrationRepository registrationRepository,
            ContestStandingRepository standingRepository,
            StandingsStore standingsStore,
            Clock clock,
            @Value("${sqljudge.contest.finalize-grace}") Duration grace) {
        this.contestRepository = contestRepository;
        this.cellRepository = cellRepository;
        this.registrationRepository = registrationRepository;
        this.standingRepository = standingRepository;
        this.standingsStore = standingsStore;
        this.clock = clock;
        this.grace = grace;
    }

    @Transactional
    public void finalizeDue() {
        Instant now = clock.instant();
        contestRepository
                .findDueForFinalization(now.minus(grace))
                .forEach(contest -> finalizeContest(contest, now));
    }

    private void finalizeContest(Contest contest, Instant now) {
        Map<UUID, ContestRegistration> registrations =
                cellRepository.findUserIdsByContestId(contest.getId()).stream()
                        .map(
                                userId ->
                                        registrationRepository.findByContestIdAndUserId(
                                                contest.getId(), userId))
                        .flatMap(Optional::stream)
                        .collect(
                                Collectors.toMap(
                                        ContestRegistration::getUserId,
                                        registration -> registration));
        List<Result> results =
                registrations.values().stream()
                        .map(registration -> resultOf(contest, registration))
                        .toList();
        List<Ranked> ranked = FinalRanks.rank(results);
        standingRepository.saveAll(
                ranked.stream()
                        .map(rank -> ContestStanding.of(registrations.get(rank.userId()), rank))
                        .toList());
        contest.markFinalized(now);
        log.info("contest_finalized contestId={} contestants={}", contest.getId(), ranked.size());
        refreshStandingsAfterCommit(contest.getId());
    }

    private Result resultOf(Contest contest, ContestRegistration registration) {
        StandingRow row =
                StandingRows.actualRow(
                        contest,
                        registration,
                        cellRepository.findByContestIdAndUserId(
                                contest.getId(), registration.getUserId()));
        return new Result(row.userId(), row.solved(), row.penaltyMinutes());
    }

    private void refreshStandingsAfterCommit(UUID contestId) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        standingsStore.discardQuietly(contestId);
                        try {
                            standingsStore.announceChanged(contestId);
                        } catch (DataAccessException unavailable) {
                            log.warn("standings_change_announce_failed contestId={}", contestId);
                        }
                    }
                });
    }
}
