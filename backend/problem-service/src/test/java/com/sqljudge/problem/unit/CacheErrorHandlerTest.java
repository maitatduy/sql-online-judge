package com.sqljudge.problem.unit;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.sqljudge.problem.config.CacheConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.RedisConnectionFailureException;

class CacheErrorHandlerTest {

    private final CacheErrorHandler handler = new CacheConfig().errorHandler();
    private final Cache cache = new ConcurrentMapCache("problem-detail");
    private final RuntimeException redisDown = new RedisConnectionFailureException("Redis down");

    @Test
    @DisplayName("Redis lỗi khi đọc, ghi, xoá cache thì chỉ ghi log, không làm hỏng request")
    void cacheErrors_areSwallowedSoRequestsFallBackToDatabase() {
        assertThatCode(
                        () -> {
                            handler.handleCacheGetError(redisDown, cache, "slug");
                            handler.handleCachePutError(redisDown, cache, "slug", "value");
                            handler.handleCacheEvictError(redisDown, cache, "slug");
                            handler.handleCacheClearError(redisDown, cache);
                        })
                .doesNotThrowAnyException();
    }
}
