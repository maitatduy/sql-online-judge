package com.sqljudge.auth.unit;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sqljudge.auth.config.JwtKeyConfig;
import com.sqljudge.auth.config.SecurityConfig;
import com.sqljudge.auth.controller.SessionController;
import com.sqljudge.auth.dto.response.AuthResponse;
import com.sqljudge.auth.dto.response.AuthUserResponse;
import com.sqljudge.auth.service.SessionService;
import com.sqljudge.auth.service.SignInResult;
import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.UnauthenticatedException;
import com.sqljudge.common.security.Role;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(SessionController.class)
@Import({SecurityConfig.class, JwtKeyConfig.class})
@ImportAutoConfiguration(CommonAutoConfiguration.class)
@TestPropertySource(
        properties = {
            "sqljudge.jwt.generate-ephemeral-key=true",
            "sqljudge.security.allowed-origins=http://localhost:3000"
        })
class SessionControllerTest {

    private static final String FRONTEND_ORIGIN = "http://localhost:3000";
    private static final AuthResponse AUTH_RESPONSE =
            new AuthResponse(
                    "new-access-token",
                    new AuthUserResponse(
                            TestJwts.USER_ID, "minh.anh", "minh.anh@gmail.com", Role.LEARNER));

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SessionService sessionService;

    @Test
    @DisplayName("Refresh từ frontend có cookie: 200, access token mới và cookie đã xoay vòng")
    void refresh_trustedOriginWithCookie_rotatesCookie() throws Exception {
        when(sessionService.refresh("old-refresh"))
                .thenReturn(new SignInResult(AUTH_RESPONSE, "new-refresh", true));

        mockMvc.perform(
                        refresh(FRONTEND_ORIGIN)
                                .cookie(new Cookie("sqljudge_refresh", "old-refresh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(cookie().value("sqljudge_refresh", "new-refresh"))
                .andExpect(cookie().maxAge("sqljudge_refresh", 14 * 24 * 60 * 60));
    }

    @Test
    @DisplayName("Refresh trong cửa sổ hai tab: chỉ trả access token, không ghi đè cookie")
    void refresh_withinGrace_doesNotSetCookie() throws Exception {
        when(sessionService.refresh("old-refresh"))
                .thenReturn(new SignInResult(AUTH_RESPONSE, null, true));

        mockMvc.perform(
                        refresh(FRONTEND_ORIGIN)
                                .cookie(new Cookie("sqljudge_refresh", "old-refresh")))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    @DisplayName("Refresh thiếu Origin hoặc từ origin lạ: 403 UNTRUSTED_ORIGIN, không đụng phiên")
    void refresh_untrustedOrigin_isRejected() throws Exception {
        Cookie cookie = new Cookie("sqljudge_refresh", "old-refresh");

        mockMvc.perform(post("/api/auth/refresh").cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNTRUSTED_ORIGIN"));
        mockMvc.perform(refresh("https://evil.example").cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNTRUSTED_ORIGIN"));

        verifyNoInteractions(sessionService);
    }

    @Test
    @DisplayName("Refresh token không hợp lệ: 401 INVALID_REFRESH_TOKEN")
    void refresh_invalidToken_returnsUnauthorized() throws Exception {
        when(sessionService.refresh(null))
                .thenThrow(
                        new UnauthenticatedException(
                                ErrorCode.INVALID_REFRESH_TOKEN, "Phiên đăng nhập đã hết hạn"));

        mockMvc.perform(refresh(FRONTEND_ORIGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    @DisplayName("Đăng xuất: 204, thu hồi phiên của cookie và xoá cookie")
    void logout_trustedOrigin_revokesAndClearsCookie() throws Exception {
        mockMvc.perform(
                        post("/api/auth/logout")
                                .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                                .cookie(new Cookie("sqljudge_refresh", "current-refresh")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("sqljudge_refresh", 0))
                .andExpect(cookie().path("sqljudge_refresh", "/api/auth"));

        verify(sessionService).revoke("current-refresh");
    }

    @Test
    @DisplayName("Đăng xuất từ origin lạ: 403, không thu hồi")
    void logout_untrustedOrigin_isRejected() throws Exception {
        mockMvc.perform(
                        post("/api/auth/logout")
                                .header(HttpHeaders.ORIGIN, "https://evil.example")
                                .cookie(new Cookie("sqljudge_refresh", "current-refresh")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(sessionService);
    }

    private static MockHttpServletRequestBuilder refresh(String origin) {
        return post("/api/auth/refresh").header(HttpHeaders.ORIGIN, origin);
    }
}
