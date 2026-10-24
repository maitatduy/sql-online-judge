package com.sqljudge.auth.unit;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.RSAKey;
import com.sqljudge.auth.config.JwtKeyConfig;
import com.sqljudge.auth.config.SecurityConfig;
import com.sqljudge.auth.controller.AdminUserDeletionController;
import com.sqljudge.auth.dto.request.DeleteUserRequest;
import com.sqljudge.auth.dto.response.AccountStatus;
import com.sqljudge.auth.dto.response.AdminUserDetailResponse;
import com.sqljudge.auth.dto.response.AdminUserSummaryResponse;
import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.service.AdminUserDeletionService;
import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.common.security.Role;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(AdminUserDeletionController.class)
@Import({SecurityConfig.class, JwtKeyConfig.class})
@ImportAutoConfiguration(CommonAutoConfiguration.class)
@TestPropertySource(
        properties = {
            "sqljudge.jwt.generate-ephemeral-key=true",
            "sqljudge.security.allowed-origins=http://localhost:3000"
        })
class AdminUserDeletionControllerTest {

    private static final String USER_PATH = "/api/admin/users/viet.luu";

    @Autowired private MockMvc mockMvc;
    @Autowired private RSAKey signingKey;

    @MockitoBean private AdminUserDeletionService adminUserDeletionService;

    @Test
    @DisplayName("Xoá tài khoản trả 204, người thực hiện lấy từ JWT")
    void delete_admin_returnsNoContent() throws Exception {
        mockMvc.perform(as("ADMIN", json(delete(USER_PATH), "{\"reason\":\"Spam\"}")))
                .andExpect(status().isNoContent());

        verify(adminUserDeletionService)
                .delete("viet.luu", new DeleteUserRequest("Spam"), TestJwts.USER_ID);
    }

    @Test
    @DisplayName("Thiếu lý do, lý do rỗng hoặc dài quá 500 ký tự: 400 trên field reason")
    void delete_invalidReason_returnsValidationError() throws Exception {
        String tooLong = "a".repeat(501);
        for (String body :
                List.of("{}", "{\"reason\":\"   \"}", "{\"reason\":\"" + tooLong + "\"}")) {
            mockMvc.perform(as("ADMIN", json(delete(USER_PATH), body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("reason"));
        }

        verifyNoInteractions(adminUserDeletionService);
    }

    @Test
    @DisplayName("Admin tự xoá chính mình: 422 CANNOT_MODIFY_SELF")
    void delete_self_returnsUnprocessable() throws Exception {
        doThrow(
                        new BusinessRuleException(
                                ErrorCode.CANNOT_MODIFY_SELF,
                                "Bạn không thể tự xoá tài khoản của chính mình"))
                .when(adminUserDeletionService)
                .delete("viet.luu", new DeleteUserRequest("Spam"), TestJwts.USER_ID);

        mockMvc.perform(as("ADMIN", json(delete(USER_PATH), "{\"reason\":\"Spam\"}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CANNOT_MODIFY_SELF"));
    }

    @Test
    @DisplayName("Không phải ADMIN bị 403, chưa đăng nhập bị 401")
    void delete_requiresAdminRole() throws Exception {
        for (String role : List.of("LEARNER", "SETTER", "MODERATOR")) {
            mockMvc.perform(as(role, json(delete(USER_PATH), "{\"reason\":\"Spam\"}")))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(json(delete(USER_PATH), "{\"reason\":\"Spam\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adminUserDeletionService);
    }

    @Test
    @DisplayName("Khôi phục tài khoản trả 200 kèm chi tiết, người thực hiện lấy từ JWT")
    void restore_admin_returnsDetail() throws Exception {
        when(adminUserDeletionService.restore("viet.luu", TestJwts.USER_ID))
                .thenReturn(restoredDetail());

        mockMvc.perform(as("ADMIN", post(USER_PATH + "/restore")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("viet.luu"))
                .andExpect(jsonPath("$.deletedAt").isEmpty());
    }

    @Test
    @DisplayName("Khôi phục tài khoản chưa bị xoá: 409 USER_NOT_DELETED")
    void restore_notDeleted_returnsConflict() throws Exception {
        when(adminUserDeletionService.restore("viet.luu", TestJwts.USER_ID))
                .thenThrow(
                        new ConflictException(ErrorCode.USER_NOT_DELETED, "Tài khoản chưa bị xoá"));

        mockMvc.perform(as("ADMIN", post(USER_PATH + "/restore")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_NOT_DELETED"));
    }

    @Test
    @DisplayName("Khôi phục: không phải ADMIN bị 403, chưa đăng nhập bị 401")
    void restore_requiresAdminRole() throws Exception {
        for (String role : List.of("LEARNER", "SETTER", "MODERATOR")) {
            mockMvc.perform(as(role, post(USER_PATH + "/restore")))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post(USER_PATH + "/restore")).andExpect(status().isUnauthorized());

        verifyNoInteractions(adminUserDeletionService);
    }

    private static AdminUserDetailResponse restoredDetail() {
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
                        AccountStatus.ACTIVE,
                        null,
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
