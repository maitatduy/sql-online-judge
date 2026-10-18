package com.sqljudge.gateway.security;

import java.util.List;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class UserHeadersFilter implements GlobalFilter, Ordered {

    static final String USER_HEADER_PREFIX = "X-User-";
    static final String USER_ID_HEADER = "X-User-Id";
    static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String ROLE_CLAIM = "role";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .ofType(Jwt.class)
                .map(jwt -> withUserHeaders(exchange, jwt))
                .defaultIfEmpty(withoutUserHeaders(exchange))
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private static ServerWebExchange withUserHeaders(ServerWebExchange exchange, Jwt jwt) {
        return exchange.mutate()
                .request(
                        request ->
                                request.headers(
                                        headers -> {
                                            removeUserHeaders(headers);
                                            headers.set(USER_ID_HEADER, jwt.getSubject());
                                            headers.set(
                                                    USER_ROLE_HEADER,
                                                    jwt.getClaimAsString(ROLE_CLAIM));
                                        }))
                .build();
    }

    private static ServerWebExchange withoutUserHeaders(ServerWebExchange exchange) {
        return exchange.mutate()
                .request(request -> request.headers(UserHeadersFilter::removeUserHeaders))
                .build();
    }

    private static void removeUserHeaders(HttpHeaders headers) {
        List<String> userHeaders =
                headers.keySet().stream()
                        .filter(
                                name ->
                                        name.regionMatches(
                                                true,
                                                0,
                                                USER_HEADER_PREFIX,
                                                0,
                                                USER_HEADER_PREFIX.length()))
                        .toList();
        userHeaders.forEach(headers::remove);
    }
}
