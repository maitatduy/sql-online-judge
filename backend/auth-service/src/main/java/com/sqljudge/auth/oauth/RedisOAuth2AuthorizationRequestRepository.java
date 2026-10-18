package com.sqljudge.auth.oauth;

import com.sqljudge.auth.service.SecureTokens;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String KEY_PREFIX = "sqljudge:auth:oauth2-request:";
    private static final Duration REQUEST_TTL = Duration.ofMinutes(10);
    private static final String NONCE_COOKIE = "sqljudge_oauth_nonce";
    private static final String NONCE_COOKIE_PATH = "/api/auth/login/oauth2";
    private static final String VALUE_SEPARATOR = ":";
    private static final ObjectInputFilter DESERIALIZATION_FILTER =
            ObjectInputFilter.Config.createFilter(
                    "maxdepth=10;maxbytes=65536;java.lang.*;java.util.*;"
                            + "org.springframework.security.oauth2.core.**;!*");

    private final StringRedisTemplate redisTemplate;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return storedValue(request)
                .map(StoredRequest::authorizationRequest)
                .map(RedisOAuth2AuthorizationRequestRepository::deserialize)
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response);
            return;
        }
        String nonce = SecureTokens.newToken();
        redisTemplate
                .opsForValue()
                .set(
                        KEY_PREFIX + authorizationRequest.getState(),
                        SecureTokens.sha256(nonce)
                                + VALUE_SEPARATOR
                                + serialize(authorizationRequest),
                        REQUEST_TTL);
        response.addHeader(HttpHeaders.SET_COOKIE, nonceCookie(nonce, REQUEST_TTL).toString());
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request, HttpServletResponse response) {
        Optional<StoredRequest> stored = storedValue(request);
        if (stored.isEmpty() || !Boolean.TRUE.equals(redisTemplate.delete(stored.get().key()))) {
            return null;
        }
        response.addHeader(HttpHeaders.SET_COOKIE, nonceCookie("", Duration.ZERO).toString());
        return deserialize(stored.get().authorizationRequest());
    }

    private Optional<StoredRequest> storedValue(HttpServletRequest request) {
        String state = request.getParameter(OAuth2ParameterNames.STATE);
        Optional<String> nonce = nonceFrom(request);
        if (state == null || nonce.isEmpty()) {
            return Optional.empty();
        }
        String key = KEY_PREFIX + state;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }
        String[] parts = value.split(VALUE_SEPARATOR, 2);
        if (parts.length != 2 || !SecureTokens.matches(nonce.get(), parts[0])) {
            log.warn("oauth_state_nonce_mismatch");
            return Optional.empty();
        }
        return Optional.of(new StoredRequest(key, parts[1]));
    }

    private static Optional<String> nonceFrom(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> NONCE_COOKIE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private static ResponseCookie nonceCookie(String value, Duration maxAge) {
        return ResponseCookie.from(NONCE_COOKIE, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path(NONCE_COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }

    private static String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(authorizationRequest);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    private static OAuth2AuthorizationRequest deserialize(String value) {
        byte[] bytes = Base64.getDecoder().decode(value);
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            input.setObjectInputFilter(DESERIALIZATION_FILTER);
            return input.readObject() instanceof OAuth2AuthorizationRequest authorizationRequest
                    ? authorizationRequest
                    : null;
        } catch (IOException | ClassNotFoundException exception) {
            log.warn(
                    "oauth_state_deserialization_rejected reason={}",
                    exception.getClass().getSimpleName());
            return null;
        }
    }

    private record StoredRequest(String key, String authorizationRequest) {}
}
