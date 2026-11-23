package com.sqljudge.contest.standings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.contest.scoring.IcpcScoring;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StandingsStore {

    public static final String CHANGED_CHANNEL = "sqljudge:contest:standings-changed";
    private static final Duration RETENTION_AFTER_END = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    static String standingsKey(UUID contestId) {
        return "sqljudge:contest:standings:" + contestId;
    }

    static String cellsKey(UUID contestId) {
        return "sqljudge:contest:cells:" + contestId;
    }

    public void put(UUID contestId, Instant endAt, StandingRow row) {
        String member = row.userId().toString();
        redisTemplate
                .opsForZSet()
                .add(
                        standingsKey(contestId),
                        member,
                        IcpcScoring.rankingScore(row.solved(), row.penaltyMinutes()));
        redisTemplate.opsForHash().put(cellsKey(contestId), member, toJson(row));
        Instant expiresAt = endAt.plus(RETENTION_AFTER_END);
        redisTemplate.expireAt(standingsKey(contestId), expiresAt);
        redisTemplate.expireAt(cellsKey(contestId), expiresAt);
        redisTemplate.convertAndSend(CHANGED_CHANNEL, contestId.toString());
    }

    public void discardQuietly(UUID contestId) {
        try {
            redisTemplate.delete(List.of(standingsKey(contestId), cellsKey(contestId)));
        } catch (DataAccessException unavailable) {
            log.warn("standings_cache_discard_failed contestId={}", contestId);
        }
    }

    public long size(UUID contestId) {
        Long size = redisTemplate.opsForZSet().zCard(standingsKey(contestId));
        return size == null ? 0 : size;
    }

    public List<RankedRow> page(UUID contestId, long offset, int size) {
        Set<TypedTuple<String>> members =
                redisTemplate
                        .opsForZSet()
                        .reverseRangeWithScores(standingsKey(contestId), offset, offset + size - 1);
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        List<Object> json =
                redisTemplate
                        .opsForHash()
                        .multiGet(
                                cellsKey(contestId),
                                members.stream()
                                        .map(member -> (Object) member.getValue())
                                        .toList());
        List<RankedRow> rows = new ArrayList<>();
        int index = 0;
        for (TypedTuple<String> member : members) {
            Object stored = json.get(index++);
            if (stored != null) {
                rows.add(new RankedRow(rankOf(contestId, member.getScore()), fromJson(stored)));
            }
        }
        return rows;
    }

    private int rankOf(UUID contestId, Double score) {
        Long better =
                redisTemplate
                        .opsForZSet()
                        .count(
                                standingsKey(contestId),
                                Objects.requireNonNull(score) + 1,
                                Double.POSITIVE_INFINITY);
        return (better == null ? 0 : better.intValue()) + 1;
    }

    private String toJson(StandingRow row) {
        try {
            return objectMapper.writeValueAsString(row);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Standing row is not serializable", exception);
        }
    }

    private StandingRow fromJson(Object stored) {
        try {
            return objectMapper.readValue(stored.toString(), StandingRow.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored standing row is not valid JSON", exception);
        }
    }

    public record RankedRow(int rank, StandingRow row) {}
}
