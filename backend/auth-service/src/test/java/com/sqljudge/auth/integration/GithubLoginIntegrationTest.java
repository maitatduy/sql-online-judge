package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.oauth.RedisOAuth2AuthorizationRequestRepository;
import com.sqljudge.auth.repository.OAuthAccountRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.security.Role;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GithubLoginIntegrationTest extends ContainerSupport {

    private static final GithubProviderStub GITHUB = GithubProviderStub.start();
    private static final HttpClient HTTP =
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    @LocalServerPort private int port;
    @Autowired private UserRepository userRepository;
    @Autowired private OAuthAccountRepository oauthAccountRepository;

    @MockitoSpyBean
    private RedisOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @DynamicPropertySource
    static void githubProperties(DynamicPropertyRegistry registry) {
        String base = GITHUB.baseUrl();
        registry.add(
                "spring.security.oauth2.client.provider.github.authorization-uri",
                () -> base + "/login/oauth/authorize");
        registry.add(
                "spring.security.oauth2.client.provider.github.token-uri",
                () -> base + "/login/oauth/access_token");
        registry.add(
                "spring.security.oauth2.client.provider.github.user-info-uri",
                () -> base + "/user");
        registry.add("sqljudge.oauth.github.emails-uri", () -> base + "/user/emails");
    }

    @Test
    @DisplayName("GitHub lần đầu với email verified: tạo user, set cookie, về /auth/callback")
    void githubLogin_newUser_createsAccountAndSignsIn() throws Exception {
        GITHUB.willReturn(910001, "Octo-Cat", "octo.cat@gmail.com", true);

        HttpResponse<String> callback = completeGithubLogin();

        assertThat(callback.statusCode()).isEqualTo(302);
        assertThat(callback.headers().firstValue("Location"))
                .hasValue("http://localhost:3000/auth/callback");
        assertThat(refreshCookie(callback).getMaxAge()).isEqualTo(14L * 24 * 60 * 60);
        User user = userRepository.findByEmail("octo.cat@gmail.com").orElseThrow();
        assertThat(user.getUsername()).isEqualTo("octo_cat");
        assertThat(user.getSignupProvider()).isEqualTo(SignupProvider.GITHUB);
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(
                        oauthAccountRepository.findByProviderAndProviderUserId(
                                SignupProvider.GITHUB, "910001"))
                .isPresent();
        assertThat(refreshWith(refreshCookie(callback).getValue()).body())
                .contains("\"username\":\"octo_cat\"");
    }

    @Test
    @DisplayName("GitHub trùng email đã đăng ký: liên kết vào tài khoản cũ, không tạo user mới")
    void githubLogin_existingEmail_linksAccount() throws Exception {
        User existing = new User();
        existing.setUsername("da.co.san");
        existing.setEmail("da.co.san@gmail.com");
        existing.setPasswordHash("$2a$12$abcdefghijklmnopqrstuuabcdefghijklmnopqrstuvwxyz012");
        existing.setRole(Role.LEARNER);
        existing.setSignupProvider(SignupProvider.EMAIL);
        existing.setEmailVerifiedAt(Instant.parse("2026-10-01T00:00:00Z"));
        userRepository.saveAndFlush(existing);
        long usersBefore = userRepository.count();
        GITHUB.willReturn(910002, "someone-else", "Da.Co.San@gmail.com", true);

        HttpResponse<String> callback = completeGithubLogin();

        assertThat(callback.headers().firstValue("Location"))
                .hasValue("http://localhost:3000/auth/callback");
        assertThat(userRepository.count()).isEqualTo(usersBefore);
        assertThat(
                        oauthAccountRepository.findByProviderAndProviderUserId(
                                SignupProvider.GITHUB, "910002"))
                .hasValueSatisfying(
                        account ->
                                assertThat(account.getUser().getUsername()).isEqualTo("da.co.san"));
    }

    @Test
    @DisplayName(
            "Email GitHub chưa verified: về /login?error=OAUTH_EMAIL_NOT_VERIFIED, không set"
                    + " cookie")
    void githubLogin_unverifiedEmail_redirectsWithError() throws Exception {
        GITHUB.willReturn(910003, "chua-verify", "chua.verify@gmail.com", false);

        HttpResponse<String> callback = completeGithubLogin();

        assertThat(callback.statusCode()).isEqualTo(302);
        assertThat(callback.headers().firstValue("Location"))
                .hasValue("http://localhost:3000/login?error=OAUTH_EMAIL_NOT_VERIFIED");
        assertThat(callback.headers().allValues("Set-Cookie"))
                .noneMatch(cookie -> cookie.startsWith("sqljudge_refresh="));
        assertThat(userRepository.findByEmail("chua.verify@gmail.com")).isEmpty();
    }

    @Test
    @DisplayName(
            "Login CSRF: kẻ xấu gửi link callback của luồng mình bắt đầu cho nạn nhân, trình duyệt"
                    + " nạn nhân không có cookie nonce nên không bị đăng nhập")
    void githubCallback_openedInAnotherBrowser_isRejected() throws Exception {
        GITHUB.willReturn(910004, "ke-xau", "ke.xau@gmail.com", true);
        HttpResponse<String> authorization = get("/api/auth/oauth2/authorization/github", null);
        String state =
                queryParameters(
                                URI.create(
                                        authorization
                                                .headers()
                                                .firstValue("Location")
                                                .orElseThrow()))
                        .get("state");

        HttpResponse<String> victimCallback =
                get("/api/auth/login/oauth2/code/github?code=abc&state=" + state, null);

        assertThat(victimCallback.headers().firstValue("Location"))
                .hasValue("http://localhost:3000/login?error=OAUTH_LOGIN_FAILED");
        assertThat(victimCallback.headers().allValues("Set-Cookie"))
                .noneMatch(cookie -> cookie.startsWith("sqljudge_refresh="));
        assertThat(userRepository.findByEmail("ke.xau@gmail.com")).isEmpty();
    }

    @Test
    @DisplayName("Callback với state không do mình phát: không đăng nhập")
    void githubCallback_unknownState_isRejected() throws Exception {
        HttpResponse<String> callback =
                get("/api/auth/login/oauth2/code/github?code=abc&state=forged-state");

        assertThat(callback.statusCode()).isEqualTo(302);
        assertThat(callback.headers().firstValue("Location"))
                .hasValue("http://localhost:3000/login?error=OAUTH_LOGIN_FAILED");
    }

    @Test
    @DisplayName(
            "Không lưu được state OAuth (Redis lỗi): về /login?error=OAUTH_LOGIN_FAILED, không lộ"
                    + " JSON 401")
    void githubAuthorization_stateStorageUnavailable_redirectsWithError() throws Exception {
        doThrow(new RedisConnectionFailureException("Unable to connect to Redis"))
                .when(authorizationRequestRepository)
                .saveAuthorizationRequest(any(), any(), any());

        HttpResponse<String> authorization = get("/api/auth/oauth2/authorization/github");

        assertThat(authorization.statusCode()).isEqualTo(302);
        assertThat(authorization.headers().firstValue("Location"))
                .hasValue("http://localhost:3000/login?error=OAUTH_LOGIN_FAILED");
    }

    private HttpResponse<String> completeGithubLogin() throws Exception {
        HttpResponse<String> authorization = get("/api/auth/oauth2/authorization/github");
        assertThat(authorization.statusCode()).isEqualTo(302);
        String location = authorization.headers().firstValue("Location").orElseThrow();
        assertThat(location).startsWith(GITHUB.baseUrl() + "/login/oauth/authorize");
        String state = queryParameters(URI.create(location)).get("state");
        return get(
                "/api/auth/login/oauth2/code/github?code=abc&state=" + state,
                nonceCookie(authorization));
    }

    private HttpResponse<String> get(String path) throws Exception {
        return get(path, null);
    }

    private HttpResponse<String> get(String path, String cookie) throws Exception {
        HttpRequest.Builder request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String nonceCookie(HttpResponse<?> authorization) {
        return authorization.headers().allValues("Set-Cookie").stream()
                .filter(cookie -> cookie.startsWith("sqljudge_oauth_nonce="))
                .map(cookie -> cookie.substring(0, cookie.indexOf(';')))
                .findFirst()
                .orElseThrow();
    }

    private HttpResponse<String> refreshWith(String refreshToken) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/refresh"))
                        .header("Origin", "http://localhost:3000")
                        .header("Cookie", "sqljudge_refresh=" + refreshToken)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpCookie refreshCookie(HttpResponse<?> response) {
        return response.headers().allValues("Set-Cookie").stream()
                .filter(cookie -> cookie.startsWith("sqljudge_refresh="))
                .map(cookie -> HttpCookie.parse(cookie).get(0))
                .findFirst()
                .orElseThrow();
    }

    private static Map<String, String> queryParameters(URI uri) {
        return Arrays.stream(uri.getRawQuery().split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(
                        Collectors.toMap(
                                pair -> pair[0],
                                pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
    }
}
