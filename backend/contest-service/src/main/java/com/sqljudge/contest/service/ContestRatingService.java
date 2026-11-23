package com.sqljudge.contest.service;

import com.sqljudge.common.outbox.OutboxWriter;
import com.sqljudge.common.persistence.UuidV7;
import com.sqljudge.contest.dto.response.RatingChangeResponse;
import com.sqljudge.contest.entity.Contest;
import com.sqljudge.contest.entity.ContestRatingChange;
import com.sqljudge.contest.entity.ContestStanding;
import com.sqljudge.contest.entity.UserRating;
import com.sqljudge.contest.event.ContestRatingUpdatedEvent;
import com.sqljudge.contest.event.ContestRatingUpdatedEvent.RatingChange;
import com.sqljudge.contest.event.ContestTopics;
import com.sqljudge.contest.repository.ContestRatingChangeRepository;
import com.sqljudge.contest.repository.ContestRepository;
import com.sqljudge.contest.repository.ContestStandingRepository;
import com.sqljudge.contest.repository.UserRatingRepository;
import com.sqljudge.contest.scoring.EloRating;
import com.sqljudge.contest.scoring.EloRating.Change;
import com.sqljudge.contest.scoring.EloRating.Contestant;
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
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContestRatingService {

    private static final int EVENT_VERSION = 1;

    private final ContestRepository contestRepository;
    private final ContestStandingRepository standingRepository;
    private final UserRatingRepository userRatingRepository;
    private final ContestRatingChangeRepository ratingChangeRepository;
    private final OutboxWriter outboxWriter;
    private final Clock clock;

    @Transactional
    public void applyDue() {
        Instant now = clock.instant();
        contestRepository.findDueForRating().forEach(contest -> apply(contest, now));
    }

    @Transactional(readOnly = true)
    public List<RatingChangeResponse> changes(String slug) {
        Contest contest =
                contestRepository
                        .findBySlug(slug)
                        .orElseThrow(ContestQueryService::contestNotFound);
        return ratingChangeRepository
                .findByContestIdOrderByRankAscUsernameAsc(contest.getId())
                .stream()
                .map(RatingChangeResponse::from)
                .toList();
    }

    private void apply(Contest contest, Instant now) {
        List<ContestStanding> standings = standingRepository.findByContestId(contest.getId());
        Map<UUID, UserRating> ratings = currentRatings(standings);
        List<Change> changes =
                EloRating.apply(
                        standings.stream()
                                .map(
                                        standing ->
                                                new Contestant(
                                                        standing.getUserId(),
                                                        standing.getRank(),
                                                        ratings.get(standing.getUserId())
                                                                .getRating()))
                                .toList());
        Map<UUID, String> usernames =
                standings.stream()
                        .collect(
                                Collectors.toMap(
                                        ContestStanding::getUserId, ContestStanding::getUsername));
        changes.forEach(change -> ratings.get(change.userId()).record(change.newRating()));
        userRatingRepository.saveAll(ratings.values());
        ratingChangeRepository.saveAll(
                changes.stream()
                        .map(
                                change ->
                                        ContestRatingChange.of(
                                                contest.getId(),
                                                usernames.get(change.userId()),
                                                change))
                        .toList());
        contest.markRatingApplied(now);
        outboxWriter.append(
                ContestTopics.RATING_UPDATED,
                contest.getId().toString(),
                new ContestRatingUpdatedEvent(
                        UuidV7.generate(),
                        now,
                        EVENT_VERSION,
                        contest.getId(),
                        changes.stream()
                                .map(
                                        change ->
                                                new RatingChange(
                                                        change.userId(),
                                                        change.newRating(),
                                                        change.delta()))
                                .toList()));
        log.info(
                "contest_rating_applied contestId={} contestants={}",
                contest.getId(),
                changes.size());
    }

    private Map<UUID, UserRating> currentRatings(List<ContestStanding> standings) {
        List<UUID> userIds = standings.stream().map(ContestStanding::getUserId).toList();
        Map<UUID, UserRating> existing =
                userRatingRepository.findByUserIdIn(userIds).stream()
                        .collect(Collectors.toMap(UserRating::getUserId, Function.identity()));
        return userIds.stream()
                .collect(
                        Collectors.toMap(
                                Function.identity(),
                                userId ->
                                        existing.getOrDefault(userId, UserRating.initial(userId))));
    }
}
