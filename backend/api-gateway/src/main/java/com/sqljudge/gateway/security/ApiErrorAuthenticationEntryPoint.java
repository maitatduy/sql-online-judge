package com.sqljudge.gateway.security;

import com.sqljudge.gateway.error.ApiErrorWriter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ApiErrorAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private static final String UNAUTHORIZED_CODE = "UNAUTHORIZED";
    private static final String UNAUTHORIZED_MESSAGE = "Bạn cần đăng nhập để tiếp tục";

    private final ApiErrorWriter apiErrorWriter;

    public ApiErrorAuthenticationEntryPoint(ApiErrorWriter apiErrorWriter) {
        this.apiErrorWriter = apiErrorWriter;
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException exception) {
        return apiErrorWriter.write(
                exchange, HttpStatus.UNAUTHORIZED, UNAUTHORIZED_CODE, UNAUTHORIZED_MESSAGE);
    }
}
