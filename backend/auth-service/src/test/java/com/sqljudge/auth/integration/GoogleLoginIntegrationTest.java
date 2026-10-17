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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GoogleLoginIntegrationTest extends ContainerSupport {

    private static final GoogleProviderStub GOOGLE = GoogleProviderStub.start();
    private static final HttpClient HTTP =
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    @LocalServerPort private int port;
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

        HttpResponse<String> authorization = get("/api/auth/oauth2/authorization/google");
        String location = authorization.headers().firstValue("Location").orElseThrow();
        Map<String, String> query = queryParameters(URI.create(location));
        HttpResponse<String> callback =
                get("/api/auth/login/oauth2/code/google?code=abc&state=" + query.get("state"));

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
    @DisplayName("Email Google chưa verified: về /login?error=OAUTH_EMAIL_NOT_VERIFIED")
    void googleLogin_unverifiedEmail_redirectsWithError() throws Exception {
        GOOGLE.willReturn("109876543211", "chua.verify.gg@gmail.com", false, "Chưa Verify");

        HttpResponse<String> authorization = get("/api/auth/oauth2/authorization/google");
        String state =
                queryParameters(
                                URI.create(
                                        authorization
                                                .headers()
                                                .firstValue("Location")
                                                .orElseThrow()))
                        .get("state");
        HttpResponse<String> callback =
                get("/api/auth/login/oauth2/code/google?code=abc&state=" + state);

        assertThat(callback.headers().firstValue("Location"))
                .hasValue("http://localhost:3000/login?error=OAUTH_EMAIL_NOT_VERIFIED");
        assertThat(userRepository.findByEmail("chua.verify.gg@gmail.com")).isEmpty();
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
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
