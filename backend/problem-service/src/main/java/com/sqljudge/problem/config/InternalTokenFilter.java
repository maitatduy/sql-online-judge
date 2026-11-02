package com.sqljudge.problem.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

class InternalTokenFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Internal-Token";
    static final String ROLE = "INTERNAL_SERVICE";
    private static final String PRINCIPAL = "internal-service";
    private static final List<GrantedAuthority> AUTHORITIES =
            List.of(new SimpleGrantedAuthority("ROLE_" + ROLE));

    private final byte[] expectedToken;

    InternalTokenFilter(String expectedToken) {
        this.expectedToken =
                expectedToken == null
                        ? new byte[0]
                        : expectedToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (matches(request.getHeader(HEADER))) {
            SecurityContextHolder.getContext()
                    .setAuthentication(
                            new PreAuthenticatedAuthenticationToken(PRINCIPAL, null, AUTHORITIES));
        }
        chain.doFilter(request, response);
    }

    private boolean matches(String providedToken) {
        return expectedToken.length > 0
                && providedToken != null
                && MessageDigest.isEqual(
                        expectedToken, providedToken.getBytes(StandardCharsets.UTF_8));
    }
}
