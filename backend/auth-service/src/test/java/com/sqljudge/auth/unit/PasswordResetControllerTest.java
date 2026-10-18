package com.sqljudge.auth.unit;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sqljudge.auth.config.JwtKeyConfig;
import com.sqljudge.auth.config.SecurityConfig;
import com.sqljudge.auth.controller.PasswordResetController;
import com.sqljudge.auth.service.PasswordResetService;
import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(PasswordResetController.class)
@Import({SecurityConfig.class, JwtKeyConfig.class})
@ImportAutoConfiguration(CommonAutoConfiguration.class)
@TestPropertySource(
        properties = {
            "sqljudge.jwt.generate-ephemeral-key=true",
            "sqljudge.security.allowed-origins=http://localhost:3000"
        })
class PasswordResetControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PasswordResetService passwordResetService;

    @Test
    @DisplayName("Khách yêu cầu đặt lại mật khẩu: luôn 202")
    void request_validEmail_returnsAccepted() throws Exception {
        mockMvc.perform(json("/api/auth/password-reset", "{\"email\":\"a@b.dev\"}"))
                .andExpect(status().isAccepted());

        verify(passwordResetService).requestReset("a@b.dev");
    }

    @Test
    @DisplayName("Yêu cầu với email sai định dạng: 400")
    void request_invalidEmail_returnsValidationError() throws Exception {
        mockMvc.perform(json("/api/auth/password-reset", "{\"email\":\"khong-phai-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));

        verifyNoInteractions(passwordResetService);
    }

    @Test
    @DisplayName("Đặt mật khẩu mới bằng link: 204")
    void confirm_validRequest_returnsNoContent() throws Exception {
        mockMvc.perform(
                        json(
                                "/api/auth/password-reset/confirm",
                                "{\"token\":\"abc\",\"newPassword\":\"matkhau-moi-2026\"}"))
                .andExpect(status().isNoContent());

        verify(passwordResetService).resetPassword("abc", "matkhau-moi-2026");
    }

    @Test
    @DisplayName("Mật khẩu mới quá phổ biến: 400 ở trường newPassword")
    void confirm_commonPassword_returnsFieldError() throws Exception {
        mockMvc.perform(
                        json(
                                "/api/auth/password-reset/confirm",
                                "{\"token\":\"abc\",\"newPassword\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("newPassword"))
                .andExpect(
                        jsonPath("$.errors[0].message")
                                .value("Mật khẩu này quá phổ biến, hãy chọn mật khẩu khác"));

        verifyNoInteractions(passwordResetService);
    }

    @Test
    @DisplayName("Link hết hạn: 422 TOKEN_INVALID_OR_EXPIRED")
    void confirm_expiredLink_returnsUnprocessable() throws Exception {
        doThrow(
                        new BusinessRuleException(
                                ErrorCode.TOKEN_INVALID_OR_EXPIRED,
                                "Liên kết không hợp lệ hoặc đã hết hạn"))
                .when(passwordResetService)
                .resetPassword("old", "matkhau-moi-2026");

        mockMvc.perform(
                        json(
                                "/api/auth/password-reset/confirm",
                                "{\"token\":\"old\",\"newPassword\":\"matkhau-moi-2026\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID_OR_EXPIRED"));
    }

    private static MockHttpServletRequestBuilder json(String path, String body) {
        return post(path).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
