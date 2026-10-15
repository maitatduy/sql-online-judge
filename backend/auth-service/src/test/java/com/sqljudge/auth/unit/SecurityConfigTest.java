package com.sqljudge.auth.unit;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.RSAKey;
import com.sqljudge.auth.config.JwtKeyConfig;
import com.sqljudge.auth.config.SecurityConfig;
import com.sqljudge.common.config.CommonAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import({SecurityConfig.class, JwtKeyConfig.class})
@ImportAutoConfiguration(CommonAutoConfiguration.class)
@TestPropertySource(properties = "sqljudge.jwt.generate-ephemeral-key=true")
class SecurityConfigTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private RSAKey signingKey;

    @Test
    @DisplayName("Gọi API khi chưa đăng nhập trả 401 dạng ApiError")
    void protectedPath_anonymous_returnsUnauthorizedApiError() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Bạn cần đăng nhập để tiếp tục"));
    }

    @Test
    @DisplayName("Đã đăng nhập thì đi qua lớp bảo mật tới tầng MVC")
    @WithMockUser
    void protectedPath_authenticated_passesSecurity() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("JWT do chính auth-service ký và còn hạn thì đi qua lớp bảo mật")
    void protectedPath_validJwt_passesSecurity() throws Exception {
        mockMvc.perform(
                        get("/api/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(TestJwts.validToken(signingKey, "LEARNER"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("JWT ký bằng khoá khác trả 401 dạng ApiError")
    void protectedPath_jwtSignedByOtherKey_returnsUnauthorized() throws Exception {
        String forged = TestJwts.validToken(TestJwts.newKey(), "ADMIN");

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(forged)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("JWT hết hạn trả 401 dạng ApiError")
    void protectedPath_expiredJwt_returnsUnauthorized() throws Exception {
        mockMvc.perform(
                        get("/api/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(TestJwts.expiredToken(signingKey))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Header X-User-* do client tự gửi không thay được JWT")
    void protectedPath_userHeadersWithoutJwt_returnsUnauthorized() throws Exception {
        mockMvc.perform(
                        get("/api/users/me")
                                .header("X-User-Id", TestJwts.USER_ID.toString())
                                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("JWKS nội bộ công khai, chỉ chứa khoá công khai")
    void jwks_anonymous_returnsPublicKeyOnly() throws Exception {
        mockMvc.perform(get("/internal/jwks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].kid").value(signingKey.getKeyID()))
                .andExpect(jsonPath("$.keys[0].n").exists())
                .andExpect(jsonPath("$.keys[0].d").doesNotExist())
                .andExpect(jsonPath("$.keys[0].p").doesNotExist());
    }

    @Test
    @DisplayName("Health check và tài liệu API không cần đăng nhập")
    void publicPaths_anonymous_areNotBlockedBySecurity() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().is(not(401)));
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().is(not(401)));
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().is(not(401)));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
