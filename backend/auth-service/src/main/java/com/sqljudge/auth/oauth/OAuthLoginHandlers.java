package com.sqljudge.auth.oauth;

import com.sqljudge.auth.config.EmailProperties;
import com.sqljudge.auth.controller.RefreshTokenCookie;
import com.sqljudge.auth.service.OAuthSignInService;
import com.sqljudge.auth.service.SignInResult;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthLoginHandlers
        implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private static final String CALLBACK_PATH = "/auth/callback";
    private static final String LOGIN_ERROR_PATH = "/login?error=";

    private final OAuthSignInService oauthSignInService;
    private final EmailProperties emailProperties;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        try {
            SignInResult result =
                    oauthSignInService.signIn(
                            OAuthProfiles.from(
                                    token.getAuthorizedClientRegistrationId(),
                                    token.getPrincipal().getAttributes()),
                            request.getHeader(HttpHeaders.USER_AGENT));
            response.addHeader(HttpHeaders.SET_COOKIE, RefreshTokenCookie.headerFor(result));
            response.sendRedirect(emailProperties.frontendUrl() + CALLBACK_PATH);
        } catch (AppException exception) {
            redirectWithError(response, exception.getCode());
        }
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException {
        log.warn("oauth_login_failed reason={}", exception.getClass().getSimpleName());
        redirectWithError(response, ErrorCode.OAUTH_LOGIN_FAILED);
    }

    private void redirectWithError(HttpServletResponse response, ErrorCode code)
            throws IOException {
        response.sendRedirect(emailProperties.frontendUrl() + LOGIN_ERROR_PATH + code.name());
    }
}
