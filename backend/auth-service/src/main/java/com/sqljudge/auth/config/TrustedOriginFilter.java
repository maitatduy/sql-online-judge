package com.sqljudge.auth.config;

import com.sqljudge.common.error.ApiError;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.web.ApiErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

class TrustedOriginFilter extends OncePerRequestFilter {

    static final Set<String> COOKIE_SESSION_PATHS = Set.of("/api/auth/refresh", "/api/auth/logout");

    private final Set<String> allowedOrigins;
    private final ApiErrorWriter apiErrorWriter;
    private final Clock clock;

    TrustedOriginFilter(
            SecurityProperties securityProperties, ApiErrorWriter apiErrorWriter, Clock clock) {
        this.allowedOrigins = Set.copyOf(securityProperties.allowedOrigins());
        this.apiErrorWriter = apiErrorWriter;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !HttpMethod.POST.matches(request.getMethod())
                || !COOKIE_SESSION_PATHS.contains(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null || !allowedOrigins.contains(origin)) {
            apiErrorWriter.write(
                    response,
                    ApiError.of(
                            HttpStatus.FORBIDDEN,
                            ErrorCode.UNTRUSTED_ORIGIN,
                            "Yêu cầu không đến từ trang SQL Judge",
                            clock.instant()));
            return;
        }
        chain.doFilter(request, response);
    }
}
