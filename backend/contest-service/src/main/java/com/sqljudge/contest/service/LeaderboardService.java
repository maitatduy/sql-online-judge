package com.sqljudge.contest.service;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.contest.client.AuthServiceClient;
import com.sqljudge.contest.client.UserName;
import com.sqljudge.contest.dto.response.LeaderboardEntryResponse;
import com.sqljudge.contest.dto.response.RatingSummaryResponse;
import com.sqljudge.contest.entity.UserRating;
import com.sqljudge.contest.repository.UserRatingRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final UserRatingRepository userRatingRepository;
    private final AuthServiceClient authServiceClient;

    public PageResponse<LeaderboardEntryResponse> leaderboard(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        Page<UserRating> ratings =
                userRatingRepository
                        .findByContestsPlayedGreaterThanOrderByRatingDescContestsPlayedDescUserIdAsc(
                                0, pageRequest);
        Map<UUID, UserName> names = namesOf(ratings.getContent());
        int offset = (int) pageRequest.getOffset();
        List<LeaderboardEntryResponse> entries =
                IntStream.range(0, ratings.getNumberOfElements())
                        .mapToObj(
                                index ->
                                        toEntry(
                                                offset + index + 1,
                                                ratings.getContent().get(index),
                                                names))
                        .toList();
        return PageResponse.from(
                new PageImpl<>(entries, pageRequest, ratings.getTotalElements()),
                Function.identity());
    }

    @Transactional(readOnly = true)
    public RatingSummaryResponse summary(UUID userId) {
        return userRatingRepository
                .findByUserId(userId)
                .filter(rating -> rating.getContestsPlayed() > 0)
                .map(
                        rating ->
                                new RatingSummaryResponse(
                                        userId,
                                        rating.getRating(),
                                        (int)
                                                        userRatingRepository
                                                                .countByContestsPlayedGreaterThanAndRatingGreaterThan(
                                                                        0, rating.getRating())
                                                + 1,
                                        rating.getContestsPlayed()))
                .orElseGet(() -> RatingSummaryResponse.unrated(userId));
    }

    private Map<UUID, UserName> namesOf(List<UserRating> ratings) {
        if (ratings.isEmpty()) {
            return Map.of();
        }
        return authServiceClient
                .userNames(ratings.stream().map(UserRating::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(UserName::userId, Function.identity(), (a, b) -> a));
    }

    private static LeaderboardEntryResponse toEntry(
            int rank, UserRating rating, Map<UUID, UserName> names) {
        UserName name = names.get(rating.getUserId());
        return new LeaderboardEntryResponse(
                rank,
                rating.getUserId(),
                name == null ? null : name.username(),
                name == null ? null : name.displayName(),
                rating.getRating(),
                rating.getContestsPlayed());
    }
}
