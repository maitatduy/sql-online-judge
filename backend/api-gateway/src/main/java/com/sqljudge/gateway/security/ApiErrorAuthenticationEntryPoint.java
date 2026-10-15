package com.sqljudge.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.gateway.error.ApiError;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ApiErrorAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private static final String UNAUTHORIZED_CODE = "UNAUTHORIZED";
    private static final String UNAUTHORIZED_MESSAGE = "Bạn cần đăng nhập để tiếp tục";

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ApiErrorAuthenticationEntryPoint(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException exception) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        ApiError error =
                new ApiError(
                        HttpStatus.UNAUTHORIZED.value(),
                        UNAUTHORIZED_CODE,
                        UNAUTHORIZED_MESSAGE,
                        clock.instant());
        return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(error))
                .map(body -> response.bufferFactory().wrap(body))
                .flatMap(buffer -> response.writeWith(Mono.just(buffer)));
    }
}
