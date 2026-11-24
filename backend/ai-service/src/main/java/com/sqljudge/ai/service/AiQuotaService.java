package com.sqljudge.ai.service;

import com.sqljudge.ai.config.AiProperties;
import com.sqljudge.ai.dto.response.QuotaResponse;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.QuotaExceededException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiQuotaService {

    private static final ZoneId QUOTA_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Duration KEY_TTL = Duration.ofHours(26);

    private final StringRedisTemplate redisTemplate;
    private final AiProperties properties;
    private final Clock clock;

    public void consume(UUID userId) {
        String key = keyOf(userId);
        Long used = redisTemplate.opsForValue().increment(key);
        if (used != null && used == 1) {
            redisTemplate.expire(key, KEY_TTL);
        }
        if (used != null && used > properties.dailyMessageLimit()) {
            redisTemplate.opsForValue().decrement(key);
            throw new QuotaExceededException(
                    ErrorCode.AI_QUOTA_EXCEEDED,
                    "Bạn đã dùng hết lượt hỏi AI hôm nay",
                    Duration.between(clock.instant(), resetsAt()));
        }
    }

    public void refund(UUID userId) {
        redisTemplate.opsForValue().decrement(keyOf(userId));
    }

    public QuotaResponse usage(UUID userId) {
        String used = redisTemplate.opsForValue().get(keyOf(userId));
        return new QuotaResponse(
                used == null ? 0 : Integer.parseInt(used),
                properties.dailyMessageLimit(),
                resetsAt());
    }

    private String keyOf(UUID userId) {
        return "sqljudge:ai:quota:" + userId + ":" + today().format(DAY);
    }

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), QUOTA_ZONE);
    }

    private Instant resetsAt() {
        return today().plusDays(1).atStartOfDay(QUOTA_ZONE).toInstant();
    }
}
