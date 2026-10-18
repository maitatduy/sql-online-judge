package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.oauth.RedisOAuth2AuthorizationRequestRepository;
import com.sqljudge.auth.service.SecureTokens;
import jakarta.servlet.http.Cookie;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@ExtendWith(MockitoExtension.class)
class RedisOAuth2AuthorizationRequestRepositoryTest {

    private static final String STATE = "state-from-provider";
    private static final String KEY = "sqljudge:auth:oauth2-request:" + STATE;
    private static final String NONCE_COOKIE = "sqljudge_oauth_nonce";

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private RedisOAuth2AuthorizationRequestRepository repository;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        repository = new RedisOAuth2AuthorizationRequestRepository(redisTemplate);
    }

    @Test
    @DisplayName(
            "Bắt đầu luồng: lưu request kèm hash của nonce, đặt cookie nonce httpOnly, Secure,"
                    + " SameSite=Lax, path hẹp")
    void save_storesRequestAndSetsNonceCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(
                authorizationRequest(), new MockHttpServletRequest(), response);

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie)
                .startsWith(NONCE_COOKIE + "=")
                .contains("Path=/api/auth/login/oauth2")
                .contains("Max-Age=600")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax");
        String nonce = setCookie.substring(NONCE_COOKIE.length() + 1, setCookie.indexOf(';'));
        assertThat(storedValue()).startsWith(SecureTokens.sha256(nonce) + ":");
    }

    @Test
    @DisplayName("Callback từ đúng trình duyệt: trả request một lần và xoá cookie nonce")
    void remove_matchingNonce_returnsRequestOnceAndClearsCookie() {
        MockHttpServletResponse startResponse = new MockHttpServletResponse();
        repository.saveAuthorizationRequest(
                authorizationRequest(), new MockHttpServletRequest(), startResponse);
        String nonce = startResponse.getCookie(NONCE_COOKIE).getValue();
        String stored = storedValue();
        when(valueOperations.get(KEY)).thenReturn(stored);
        when(redisTemplate.delete(KEY)).thenReturn(true);
        MockHttpServletResponse callbackResponse = new MockHttpServletResponse();

        OAuth2AuthorizationRequest loaded =
                repository.removeAuthorizationRequest(callback(nonce), callbackResponse);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getState()).isEqualTo(STATE);
        assertThat(callbackResponse.getHeader("Set-Cookie"))
                .startsWith(NONCE_COOKIE + "=;")
                .contains("Max-Age=0");
    }

    @Test
    @DisplayName(
            "Callback mở ở trình duyệt khác (thiếu hoặc sai nonce): không trả request, giữ"
                    + " nguyên state")
    void remove_missingOrWrongNonce_returnsNull() {
        repository.saveAuthorizationRequest(
                authorizationRequest(),
                new MockHttpServletRequest(),
                new MockHttpServletResponse());
        String stored = storedValue();
        when(valueOperations.get(KEY)).thenReturn(stored);

        assertThat(repository.removeAuthorizationRequest(callback(null), response())).isNull();
        assertThat(repository.removeAuthorizationRequest(callback("nonce-gia"), response()))
                .isNull();
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("Hai callback cùng state: chỉ lần xoá được khoá mới nhận request")
    void remove_alreadyConsumed_returnsNull() {
        MockHttpServletResponse startResponse = new MockHttpServletResponse();
        repository.saveAuthorizationRequest(
                authorizationRequest(), new MockHttpServletRequest(), startResponse);
        String nonce = startResponse.getCookie(NONCE_COOKIE).getValue();
        String stored = storedValue();
        when(valueOperations.get(KEY)).thenReturn(stored);
        when(redisTemplate.delete(KEY)).thenReturn(false);

        assertThat(repository.removeAuthorizationRequest(callback(nonce), response())).isNull();
    }

    @Test
    @DisplayName(
            "Dữ liệu trong Redis chứa class ngoài danh sách cho phép: từ chối, không dựng object")
    void load_disallowedClass_returnsNull() throws Exception {
        String nonce = "nonce-hop-le";
        when(valueOperations.get(KEY))
                .thenReturn(SecureTokens.sha256(nonce) + ":" + serialize(new Gadget("payload")));

        assertThat(repository.loadAuthorizationRequest(callback(nonce))).isNull();
    }

    private String storedValue() {
        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(KEY), value.capture(), eq(Duration.ofMinutes(10)));
        return value.getValue();
    }

    private static MockHttpServletRequest callback(String nonce) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("state", STATE);
        if (nonce != null) {
            request.setCookies(new Cookie(NONCE_COOKIE, nonce));
        }
        return request;
    }

    private static MockHttpServletResponse response() {
        return new MockHttpServletResponse();
    }

    private static OAuth2AuthorizationRequest authorizationRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://github.com/login/oauth/authorize")
                .clientId("client-id")
                .redirectUri("http://localhost:8080/api/auth/login/oauth2/code/github")
                .state(STATE)
                .build();
    }

    private static String serialize(Serializable value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(value);
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    private record Gadget(String command) implements Serializable {}
}
