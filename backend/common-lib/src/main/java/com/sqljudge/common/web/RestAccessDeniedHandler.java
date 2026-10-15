package com.sqljudge.common.web;

import com.sqljudge.common.error.ApiError;
import com.sqljudge.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ApiErrorWriter writer;
    private final Clock clock;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {
        writer.write(
                response,
                ApiError.of(
                        HttpStatus.FORBIDDEN,
                        ErrorCode.ACCESS_DENIED,
                        SecurityMessages.ACCESS_DENIED,
                        clock.instant()));
    }
}
