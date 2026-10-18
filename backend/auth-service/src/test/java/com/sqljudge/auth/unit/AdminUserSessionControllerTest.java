package com.sqljudge.auth.unit;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.RSAKey;
import com.sqljudge.auth.config.JwtKeyConfig;
import com.sqljudge.auth.config.SecurityConfig;
import com.sqljudge.auth.controller.AdminUserSessionController;
import com.sqljudge.auth.dto.response.UserSessionResponse;
import com.sqljudge.auth.dto.response.UserSessionsResponse;
import com.sqljudge.auth.service.AdminUserSessionService;
import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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

@WebMvcTest(AdminUserSessionController.class)
@Import({SecurityConfig.class, JwtKeyConfig.class})
@ImportAutoConfiguration(CommonAutoConfiguration.class)
@TestPropertySource(
        properties = {
            "sqljudge.jwt.generate-ephemeral-key=true",
            "sqljudge.security.allowed-origins=http://localhost:3000"
        })
class AdminUserSessionControllerTest {

    private static final String SESSIONS_PATH = "/api/admin/users/viet.luu/sessions";
    private static final UUID SESSION_ID = UUID.fromString("0192f3a0-0000-7000-8000-0000000000c1");

    @Autowired private MockMvc mockMvc;
    @Autowired private RSAKey signingKey;

    @MockitoBean private AdminUserSessionService adminUserSessionService;

    @Test
    @DisplayName("Admin xem phiên: trả items, city để trống khi chưa có dữ liệu vị trí")
    void listSessions_admin_returnsItems() throws Exception {
        when(adminUserSessionService.listSessions("viet.luu"))
                .thenReturn(
                        new UserSessionsResponse(
                                List.of(
                                        new UserSessionResponse(
                                                SESSION_ID,
                                                "Chrome",
                                                "Windows",
                                                null,
                                                Instant.parse("2026-10-18T04:57:00Z"),
                                                true,
                                                false))));

        mockMvc.perform(as("ADMIN", get(SESSIONS_PATH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$.items[0].browser").value("Chrome"))
                .andExpect(jsonPath("$.items[0].os").value("Windows"))
                .andExpect(jsonPath("$.items[0].city").isEmpty())
                .andExpect(jsonPath("$.items[0].lastActiveAt").value("2026-10-18T04:57:00Z"))
                .andExpect(jsonPath("$.items[0].isActiveNow").value(true))
                .andExpect(jsonPath("$.items[0].isUnusualLocation").value(false));
    }

    @Test
    @DisplayName("Đăng xuất một phiên trả 204, người thực hiện lấy từ JWT")
    void revokeSession_admin_returnsNoContent() throws Exception {
        mockMvc.perform(as("ADMIN", delete(SESSIONS_PATH + "/" + SESSION_ID)))
                .andExpect(status().isNoContent());

        verify(adminUserSessionService).revokeSession("viet.luu", SESSION_ID, TestJwts.USER_ID);
    }

    @Test
    @DisplayName("Phiên không có trả 404 SESSION_NOT_FOUND; id không phải UUID trả 400")
    void revokeSession_unknownOrMalformed() throws Exception {
        doThrow(
                        new NotFoundException(
                                ErrorCode.SESSION_NOT_FOUND, "Không tìm thấy phiên đăng nhập"))
                .when(adminUserSessionService)
                .revokeSession("viet.luu", SESSION_ID, TestJwts.USER_ID);

        mockMvc.perform(as("ADMIN", delete(SESSIONS_PATH + "/" + SESSION_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
        mockMvc.perform(as("ADMIN", delete(SESSIONS_PATH + "/khong-phai-uuid")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Đăng xuất tất cả phiên trả 204")
    void revokeAllSessions_admin_returnsNoContent() throws Exception {
        mockMvc.perform(as("ADMIN", delete(SESSIONS_PATH))).andExpect(status().isNoContent());

        verify(adminUserSessionService).revokeAllSessions("viet.luu", TestJwts.USER_ID);
    }

    @Test
    @DisplayName("Không phải ADMIN bị 403, chưa đăng nhập bị 401")
    void sessionEndpoints_requireAdminRole() throws Exception {
        for (String role : List.of("LEARNER", "SETTER", "MODERATOR")) {
            mockMvc.perform(as(role, get(SESSIONS_PATH))).andExpect(status().isForbidden());
            mockMvc.perform(as(role, delete(SESSIONS_PATH + "/" + SESSION_ID)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(as(role, delete(SESSIONS_PATH))).andExpect(status().isForbidden());
        }
        mockMvc.perform(get(SESSIONS_PATH)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(SESSIONS_PATH)).andExpect(status().isUnauthorized());

        verifyNoInteractions(adminUserSessionService);
    }

    private MockHttpServletRequestBuilder as(String role, MockHttpServletRequestBuilder request) {
        return request.header(
                HttpHeaders.AUTHORIZATION, "Bearer " + TestJwts.validToken(signingKey, role));
    }
}
