package com.sqljudge.auth.unit;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.RSAKey;
import com.sqljudge.auth.config.JwtKeyConfig;
import com.sqljudge.auth.config.SecurityConfig;
import com.sqljudge.auth.controller.AdminUserEmailController;
import com.sqljudge.auth.dto.response.UserEmailResponse;
import com.sqljudge.auth.service.AdminUserEmailService;
import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.common.exception.QuotaExceededException;
import java.time.Duration;
import java.util.List;
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

@WebMvcTest(AdminUserEmailController.class)
@Import({SecurityConfig.class, JwtKeyConfig.class})
@ImportAutoConfiguration(CommonAutoConfiguration.class)
@TestPropertySource(
        properties = {
            "sqljudge.jwt.generate-ephemeral-key=true",
            "sqljudge.security.allowed-origins=http://localhost:3000"
        })
class AdminUserEmailControllerTest {

    private static final String RESET_PATH = "/api/admin/users/viet.luu/password-reset-email";
    private static final String VERIFY_PATH = "/api/admin/users/viet.luu/verification-email";

    @Autowired private MockMvc mockMvc;
    @Autowired private RSAKey signingKey;

    @MockitoBean private AdminUserEmailService adminUserEmailService;

    @Test
    @DisplayName("Gửi link đặt lại mật khẩu trả 202 kèm username, người gửi lấy từ JWT")
    void sendPasswordResetEmail_admin_returnsAccepted() throws Exception {
        when(adminUserEmailService.sendPasswordResetEmail("viet.luu", TestJwts.USER_ID))
                .thenReturn(new UserEmailResponse("viet.luu"));

        mockMvc.perform(as("ADMIN", post(RESET_PATH)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.username").value("viet.luu"));
    }

    @Test
    @DisplayName("Gửi lại email xác minh trả 202 kèm username")
    void sendVerificationEmail_admin_returnsAccepted() throws Exception {
        when(adminUserEmailService.sendVerificationEmail("viet.luu", TestJwts.USER_ID))
                .thenReturn(new UserEmailResponse("viet.luu"));

        mockMvc.perform(as("ADMIN", post(VERIFY_PATH)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.username").value("viet.luu"));
    }

    @Test
    @DisplayName("Email đã xác minh trả 409; vừa gửi xong trả 429 kèm Retry-After")
    void sendEmails_conflictOrCooldown() throws Exception {
        when(adminUserEmailService.sendVerificationEmail("viet.luu", TestJwts.USER_ID))
                .thenThrow(
                        new ConflictException(
                                ErrorCode.EMAIL_ALREADY_VERIFIED, "Email đã được xác minh"));
        when(adminUserEmailService.sendPasswordResetEmail("viet.luu", TestJwts.USER_ID))
                .thenThrow(
                        new QuotaExceededException(
                                ErrorCode.EMAIL_RESEND_COOLDOWN,
                                "Vừa gửi email, vui lòng chờ",
                                Duration.ofSeconds(42)));

        mockMvc.perform(as("ADMIN", post(VERIFY_PATH)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_VERIFIED"));
        mockMvc.perform(as("ADMIN", post(RESET_PATH)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "42"))
                .andExpect(jsonPath("$.code").value("EMAIL_RESEND_COOLDOWN"));
    }

    @Test
    @DisplayName("Không phải ADMIN bị 403, chưa đăng nhập bị 401")
    void emailEndpoints_requireAdminRole() throws Exception {
        for (String role : List.of("LEARNER", "SETTER", "MODERATOR")) {
            mockMvc.perform(as(role, post(RESET_PATH))).andExpect(status().isForbidden());
            mockMvc.perform(as(role, post(VERIFY_PATH))).andExpect(status().isForbidden());
        }
        mockMvc.perform(post(RESET_PATH)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(VERIFY_PATH)).andExpect(status().isUnauthorized());

        verifyNoInteractions(adminUserEmailService);
    }

    private MockHttpServletRequestBuilder as(String role, MockHttpServletRequestBuilder request) {
        return request.header(
                HttpHeaders.AUTHORIZATION, "Bearer " + TestJwts.validToken(signingKey, role));
    }
}
