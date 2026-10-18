package com.sqljudge.gateway.ratelimit;

import com.sqljudge.gateway.error.ApiErrorWriter;
import com.sqljudge.gateway.ratelimit.RateLimitProperties.Policy;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ipresolver.RemoteAddressResolver;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.core.Ordered;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

@Component
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String KEY_PREFIX = "sqljudge:gateway:ratelimit:";
    private static final String UNKNOWN_CLIENT = "unknown";
    private static final String RATE_LIMITED_CODE = "RATE_LIMITED";
    private static final String RATE_LIMITED_MESSAGE =
            "Bạn thao tác quá nhanh, vui lòng thử lại sau";

    private final List<CompiledPolicy> policies;
    private final RemoteAddressResolver clientAddressResolver;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ApiErrorWriter apiErrorWriter;

    public RateLimitFilter(
            RateLimitProperties properties,
            ReactiveStringRedisTemplate redisTemplate,
            ApiErrorWriter apiErrorWriter) {
        this.clientAddressResolver =
                properties.trustedProxies() == 0
                        ? new RemoteAddressResolver() {}
                        : XForwardedRemoteAddressResolver.maxTrustedIndex(
                                properties.trustedProxies());
        this.policies =
                properties.policies().stream()
                        .map(
                                policy ->
                                        new CompiledPolicy(
                                                policy,
                                                PathPatternParser.defaultInstance.parse(
                                                        policy.path())))
                        .toList();
        this.redisTemplate = redisTemplate;
        this.apiErrorWriter = apiErrorWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return matchingPolicy(exchange)
                .map(policy -> limit(exchange, chain, policy))
                .orElseGet(() -> chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    private Optional<Policy> matchingPolicy(ServerWebExchange exchange) {
        if (exchange.getRequest().getMethod() != HttpMethod.POST) {
            return Optional.empty();
        }
        PathContainer path = exchange.getRequest().getPath().pathWithinApplication();
        return policies.stream()
                .filter(compiled -> compiled.pattern().matches(path))
                .map(CompiledPolicy::policy)
                .findFirst();
    }

    private Mono<Void> limit(ServerWebExchange exchange, GatewayFilterChain chain, Policy policy) {
        String key = KEY_PREFIX + policy.name() + ":" + clientAddress(exchange);
        return countRequest(key, policy.window())
                .flatMap(
                        count ->
                                count > policy.limit()
                                        ? reject(exchange, key, policy.window())
                                        : chain.filter(exchange));
    }

    private Mono<Long> countRequest(String key, Duration window) {
        return redisTemplate
                .opsForValue()
                .increment(key)
                .flatMap(
                        count ->
                                count == 1
                                        ? redisTemplate.expire(key, window).thenReturn(count)
                                        : Mono.just(count))
                .onErrorResume(
                        DataAccessException.class,
                        exception -> {
                            log.warn(
                                    "rate_limiter_unavailable reason={}",
                                    exception.getClass().getSimpleName());
                            return Mono.just(0L);
                        });
    }

    private Mono<Void> reject(ServerWebExchange exchange, String key, Duration window) {
        return redisTemplate
                .getExpire(key)
                .filter(ttl -> !ttl.isNegative() && !ttl.isZero())
                .defaultIfEmpty(window)
                .onErrorReturn(window)
                .flatMap(
                        retryAfter -> {
                            exchange.getResponse()
                                    .getHeaders()
                                    .set(
                                            HttpHeaders.RETRY_AFTER,
                                            String.valueOf(Math.max(1, retryAfter.toSeconds())));
                            return apiErrorWriter.write(
                                    exchange,
                                    HttpStatus.TOO_MANY_REQUESTS,
                                    RATE_LIMITED_CODE,
                                    RATE_LIMITED_MESSAGE);
                        });
    }

    private String clientAddress(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = clientAddressResolver.resolve(exchange);
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return UNKNOWN_CLIENT;
        }
        return remoteAddress.getAddress().getHostAddress();
    }

    private record CompiledPolicy(Policy policy, PathPattern pattern) {}
}
