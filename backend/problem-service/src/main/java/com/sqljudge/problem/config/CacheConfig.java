package com.sqljudge.problem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.problem.dto.response.ProblemDetailResponse;
import com.sqljudge.problem.dto.response.TagsResponse;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    public static final String PROBLEM_DETAIL = "problem-detail";
    public static final String PROBLEM_TAGS = "problem-tags";
    public static final String TAGS_KEY = "'tags'";

    private static final Duration PROBLEM_DETAIL_TTL = Duration.ofMinutes(10);
    private static final Duration PROBLEM_TAGS_TTL = Duration.ofHours(1);

    @Bean
    RedisCacheManagerBuilderCustomizer problemCaches(ObjectMapper objectMapper) {
        return builder ->
                builder.withCacheConfiguration(
                                PROBLEM_DETAIL,
                                cacheOf(
                                        "sqljudge:problem:detail:",
                                        PROBLEM_DETAIL_TTL,
                                        new Jackson2JsonRedisSerializer<>(
                                                objectMapper, ProblemDetailResponse.class)))
                        .withCacheConfiguration(
                                PROBLEM_TAGS,
                                cacheOf(
                                        "sqljudge:problem:",
                                        PROBLEM_TAGS_TTL,
                                        new Jackson2JsonRedisSerializer<>(
                                                objectMapper, TagsResponse.class)));
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new FallThroughCacheErrorHandler();
    }

    private static RedisCacheConfiguration cacheOf(
            String keyPrefix, Duration ttl, Jackson2JsonRedisSerializer<?> serializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .computePrefixWith(cacheName -> keyPrefix)
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(serializer));
    }

    static class FallThroughCacheErrorHandler implements CacheErrorHandler {

        @Override
        public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
            log.warn("cache_get_failed cache={} key={}", cache.getName(), key, exception);
        }

        @Override
        public void handleCachePutError(
                RuntimeException exception, Cache cache, Object key, Object value) {
            log.warn("cache_put_failed cache={} key={}", cache.getName(), key, exception);
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
            log.warn("cache_evict_failed cache={} key={}", cache.getName(), key, exception);
        }

        @Override
        public void handleCacheClearError(RuntimeException exception, Cache cache) {
            log.warn("cache_clear_failed cache={}", cache.getName(), exception);
        }
    }
}
