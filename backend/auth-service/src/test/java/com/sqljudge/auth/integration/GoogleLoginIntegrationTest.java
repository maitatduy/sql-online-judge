package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.OAuthAccountRepository;
import com.sqljudge.auth.repository.UserRepository;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GoogleLoginIntegrationTest extends ContainerSupport {

    private static final GoogleProviderStub GOOGLE = GoogleProviderStub.start();
    private static final HttpClient HTTP =
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    private static final String ATTACKER_PASSWORD = "matkhau-ke-xau-2026";

    @LocalServerPort private int port;
    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private OAuthAccountRepository oauthAccountRepository;

    @DynamicPropertySource
    static void googleProperties(DynamicPropertyRegistry registry) {
        String base = GOOGLE.baseUrl();
        registry.add(
                "spring.security.oauth2.client.provider.google.authorization-uri",
                () -> base + "/o/oauth2/v2/auth");
        registry.add(
                "spring.security.oauth2.client.provider.google.token-uri", () -> base + "/token");
        registry.add(
                "spring.security.oauth2.client.provider.google.user-info-uri",
                () -> base + "/v3/userinfo");
    }

    @Test
    @DisplayName(
            "Google lần đầu với email verified: tạo user, xin đúng scope profile email, về"
                    + " /auth/callback")
    void googleLogin_newUser_createsAccountAndSignsIn() throws Exception {
        GOOGLE.willReturn("109876543210", "Gg.User@gmail.com", true, "Người Dùng Google");

        HttpResponse<String> authorization = get("/api/auth/oauth2/authorization/google", null);
        String location = authorization.headers().firstValue("Location").orElseThrow();
        Map<String, String> query = queryParameters(URI.create(location));
        HttpResponse<String> callback = callback(authorization);

        assertThat(location).startsWith(GOOGLE.baseUrl() + "/o/oauth2/v2/auth");
        assertThat(query.get("scope")).isEqualTo("profile email");
        assertThat(callback.statusCode()).isEqualTo(302);
        assertThat(callback.headers().firstValue("Location"))
                .hasValue("http://localhost:3000/auth/callback");
        User user = userRepository.findByEmail("gg.user@gmail.com").orElseThrow();
        assertThat(user.getUsername()).isEqualTo("gg.user");
        assertThat(user.getDisplayName()).isEqualTo("Người Dùng Google");
        assertThat(user.getSignupProvider()).isEqualTo(SignupProvider.GOOGLE);
        assertThat(
                        oauthAccountRepository.findByProviderAndProviderUserId(
                                SignupProvider.GOOGLE, "109876543210"))
                .isPresent();
    }

    @Test
    @DisplayName(
            "Kẻ xấu đăng ký trước bằng email nạn nhân: sau khi nạn nhân vào bằng Google, mật"
                    + " khẩu của kẻ xấu không còn đăng nhập được")
    void googleLogin_linksUnverifiedUser_discardsPreRegisteredPassword() throws Exception {
        String email = "nan.nhan@gmail.com";
        restTemplate.postForEntity(
                "/api/auth/register",
                Map.of(
                        "username",
                        "ke.xau",
                        "email",
                        email,
                        "password",
                        ATTACKER_PASSWORD,
                        "acceptedTerms",
                        true),
                Map.class);
        GOOGLE.willReturn("109876543212", email, true, "Nạn Nhân");

        signInWithGoogle();

        assertThat(userRepository.findByEmail(email).orElseThrow().isEmailVerified()).isTrue();
        ResponseEntity<Map> attackerLogin =
                restTemplate.postForEntity(
                        "/api/auth/login",
                        Map.of("email", email, "password", ATTACKER_PASSWORD, "rememberMe", false),
                        Map.class);
        assertThat(attackerLogin.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(attackerLogin.getBody()).containsEntry("code", "INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("Email Google chưa verified: về /login?error=OAUTH_EMAIL_NOT_VERIFIED")
    void googleLogin_unverifiedEmail_redirectsWithError() throws Exception {
        GOOGLE.willReturn("109876543211", "chua.verify.gg@gmail.com", false, "Chưa Verify");

        HttpResponse<String> callback =
                callback(get("/api/auth/oauth2/authorization/google", null));

        assertThat(callback.headers().firstValue("Location"))
                .hasValue("http://localhost:3000/login?error=OAUTH_EMAIL_NOT_VERIFIED");
        assertThat(userRepository.findByEmail("chua.verify.gg@gmail.com")).isEmpty();
    }

    private void signInWithGoogle() throws Exception {
        HttpResponse<String> callback =
                callback(get("/api/auth/oauth2/authorization/google", null));
        assertThat(callback.headers().firstValue("Location"))
                .hasValue("http://localhost:3000/auth/callback");
    }

    private HttpResponse<String> callback(HttpResponse<String> authorization) throws Exception {
        String location = authorization.headers().firstValue("Location").orElseThrow();
        String state = queryParameters(URI.create(location)).get("state");
        String nonceCookie =
                authorization.headers().allValues("Set-Cookie").stream()
                        .filter(cookie -> cookie.startsWith("sqljudge_oauth_nonce="))
                        .map(cookie -> cookie.substring(0, cookie.indexOf(';')))
                        .findFirst()
                        .orElseThrow();
        return get("/api/auth/login/oauth2/code/google?code=abc&state=" + state, nonceCookie);
    }

    private HttpResponse<String> get(String path, String cookie) throws Exception {
        HttpRequest.Builder request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
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
