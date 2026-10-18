package com.sqljudge.auth.unit;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.RSAKey;
import com.sqljudge.auth.config.JwtKeyConfig;
import com.sqljudge.auth.config.SecurityConfig;
import com.sqljudge.auth.controller.AdminUserController;
import com.sqljudge.auth.dto.request.BanDuration;
import com.sqljudge.auth.dto.request.BanUserRequest;
import com.sqljudge.auth.dto.request.UnbanUserRequest;
import com.sqljudge.auth.dto.response.AccountStatus;
import com.sqljudge.auth.dto.response.AdminUserDetailResponse;
import com.sqljudge.auth.dto.response.AdminUserSummaryResponse;
import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.service.AdminUserBanService;
import com.sqljudge.auth.service.AdminUserCommandService;
import com.sqljudge.auth.service.AdminUserQueryService;
import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.common.security.Role;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(AdminUserController.class)
@Import({SecurityConfig.class, JwtKeyConfig.class})
@ImportAutoConfiguration(CommonAutoConfiguration.class)
@TestPropertySource(
        properties = {
            "sqljudge.jwt.generate-ephemeral-key=true",
            "sqljudge.security.allowed-origins=http://localhost:3000"
        })
class AdminUserBanControllerTest {

    private static final String BAN_PATH = "/api/admin/users/viet.luu/ban";
    private static final String UNBAN_PATH = "/api/admin/users/viet.luu/unban";
    private static final String VALID_BAN =
            "{\"duration\":\"DAYS_7\",\"reason\":\"Spam bình luận\",\"notifyByEmail\":true}";

    @Autowired private MockMvc mockMvc;
    @Autowired private RSAKey signingKey;

    @MockitoBean private AdminUserQueryService adminUserQueryService;
    @MockitoBean private AdminUserCommandService adminUserCommandService;
    @MockitoBean private AdminUserBanService adminUserBanService;

    @Test
    @DisplayName("Admin khoá tài khoản: người thực hiện lấy từ JWT, trả chi tiết đã khoá")
    void banUser_admin_returnsBannedDetail() throws Exception {
        Instant bannedUntil = Instant.parse("2026-10-25T05:00:00Z");
        when(adminUserBanService.ban(
                        "viet.luu",
                        new BanUserRequest(BanDuration.DAYS_7, "Spam bình luận", true),
                        TestJwts.USER_ID))
                .thenReturn(detail(AccountStatus.BANNED, bannedUntil));

        mockMvc.perform(as("ADMIN", json(post(BAN_PATH), VALID_BAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BANNED"))
                .andExpect(jsonPath("$.bannedUntil").value("2026-10-25T05:00:00Z"));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
            delimiter = '|',
            value = {
                "reason|{\"duration\":\"DAYS_7\",\"reason\":\"   \",\"notifyByEmail\":true}",
                "duration|{\"duration\":\"DAYS_3\",\"reason\":\"Spam\",\"notifyByEmail\":true}",
                "duration|{\"reason\":\"Spam\",\"notifyByEmail\":true}",
                "notifyByEmail|{\"duration\":\"DAYS_7\",\"reason\":\"Spam\",\"notifyByEmail\":\"yes\"}",
                "notifyByEmail|{\"duration\":\"DAYS_7\",\"reason\":\"Spam\"}"
            })
    @DisplayName("Dữ liệu khoá sai trả 400 VALIDATION_FAILED đúng trường")
    void banUser_invalidField_returnsFieldError(String field, String body) throws Exception {
        mockMvc.perform(as("ADMIN", json(post(BAN_PATH), body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value(field));

        verifyNoInteractions(adminUserBanService);
    }

    @Test
    @DisplayName("Lý do quá 500 ký tự trả 400 ở trường reason")
    void banUser_reasonTooLong_returnsFieldError() throws Exception {
        String body =
                "{\"duration\":\"DAYS_7\",\"reason\":\"%s\",\"notifyByEmail\":true}"
                        .formatted("a".repeat(501));

        mockMvc.perform(as("ADMIN", json(post(BAN_PATH), body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reason"));
    }

    @Test
    @DisplayName("Admin mở khoá: trả chi tiết đang hoạt động")
    void unbanUser_admin_returnsActiveDetail() throws Exception {
        when(adminUserBanService.unban(
                        eq("viet.luu"),
                        eq(new UnbanUserRequest("Khoá nhầm")),
                        eq(TestJwts.USER_ID)))
                .thenReturn(detail(AccountStatus.ACTIVE, null));

        mockMvc.perform(as("ADMIN", json(post(UNBAN_PATH), "{\"reason\":\"Khoá nhầm\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Mở khoá thiếu lý do trả 400 ở trường reason")
    void unbanUser_blankReason_returnsFieldError() throws Exception {
        mockMvc.perform(as("ADMIN", json(post(UNBAN_PATH), "{\"reason\":\"\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reason"));

        verifyNoInteractions(adminUserBanService);
    }

    @Test
    @DisplayName("Khoá và mở khoá: không phải ADMIN bị 403, chưa đăng nhập bị 401")
    void banEndpoints_requireAdminRole() throws Exception {
        for (String role : List.of("LEARNER", "SETTER", "MODERATOR")) {
            mockMvc.perform(as(role, json(post(BAN_PATH), VALID_BAN)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(as(role, json(post(UNBAN_PATH), "{\"reason\":\"Thử\"}")))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(json(post(BAN_PATH), VALID_BAN)).andExpect(status().isUnauthorized());
        mockMvc.perform(json(post(UNBAN_PATH), "{\"reason\":\"Thử\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adminUserBanService);
    }

    private static AdminUserDetailResponse detail(AccountStatus status, Instant bannedUntil) {
        return new AdminUserDetailResponse(
                new AdminUserSummaryResponse(
                        UUID.fromString("0192f3a0-0000-7000-8000-00000000000b"),
                        "viet.luu",
                        "viet.luu@gmail.com",
                        Role.LEARNER,
                        0,
                        0,
                        Instant.parse("2026-09-01T00:00:00Z"),
                        null,
                        false,
                        status,
                        bannedUntil,
                        true,
                        null,
                        null),
                null,
                null,
                SignupProvider.EMAIL,
                0,
                0,
                List.of());
    }

    private static MockHttpServletRequestBuilder json(
            MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockHttpServletRequestBuilder as(String role, MockHttpServletRequestBuilder request) {
        return request.header(
                HttpHeaders.AUTHORIZATION, "Bearer " + TestJwts.validToken(signingKey, role));
    }
}
