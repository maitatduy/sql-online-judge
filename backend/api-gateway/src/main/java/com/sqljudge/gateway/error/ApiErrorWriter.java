package com.sqljudge.gateway.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ApiErrorWriter {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ApiErrorWriter(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public Mono<Void> write(
            ServerWebExchange exchange, HttpStatus status, String code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        ApiError error = new ApiError(status.value(), code, message, clock.instant());
        return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(error))
                .map(body -> response.bufferFactory().wrap(body))
                .flatMap(buffer -> response.writeWith(Mono.just(buffer)));
    }
}
