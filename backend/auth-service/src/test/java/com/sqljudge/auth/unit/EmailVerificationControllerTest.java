package com.sqljudge.auth.unit;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sqljudge.auth.config.JwtKeyConfig;
import com.sqljudge.auth.config.SecurityConfig;
import com.sqljudge.auth.controller.EmailVerificationController;
import com.sqljudge.auth.dto.response.AuthResponse;
import com.sqljudge.auth.dto.response.AuthUserResponse;
import com.sqljudge.auth.service.EmailVerificationService;
import com.sqljudge.auth.service.SignInResult;
import com.sqljudge.auth.service.SignInService;
import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.security.Role;
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

@WebMvcTest(EmailVerificationController.class)
@Import({SecurityConfig.class, JwtKeyConfig.class})
@ImportAutoConfiguration(CommonAutoConfiguration.class)
@TestPropertySource(
        properties = {
            "sqljudge.jwt.generate-ephemeral-key=true",
            "sqljudge.security.allowed-origins=http://localhost:3000"
        })
class EmailVerificationControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private EmailVerificationService emailVerificationService;
    @MockitoBean private SignInService signInService;

    private static SignInResult signInResult() {
        return new SignInResult(
                new AuthResponse(
                        "access-token",
                        new AuthUserResponse(
                                TestJwts.USER_ID, "minh.anh", "minh.anh@gmail.com", Role.LEARNER)),
                "refresh-token",
                false);
    }

    @Test
    @DisplayName("Xác thực bằng link: tự đăng nhập (phiên không ghi nhớ), trả token và cookie")
    void confirm_withToken_signsInWithSessionCookie() throws Exception {
        when(emailVerificationService.confirmByToken("abc")).thenReturn(TestJwts.USER_ID);
        when(signInService.signIn(TestJwts.USER_ID, false, "JUnit")).thenReturn(signInResult());

        mockMvc.perform(confirm("{\"token\":\"abc\"}").header(HttpHeaders.USER_AGENT, "JUnit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.user.username").value("minh.anh"))
                .andExpect(cookie().value("sqljudge_refresh", "refresh-token"))
                .andExpect(cookie().maxAge("sqljudge_refresh", -1));
    }

    @Test
    @DisplayName("Xác thực bằng email và mã 6 số: tự đăng nhập")
    void confirm_withEmailAndCode_signsIn() throws Exception {
        when(emailVerificationService.confirmByCode("a@b.dev", "482913"))
                .thenReturn(TestJwts.USER_ID);
        when(signInService.signIn(TestJwts.USER_ID, false, "JUnit")).thenReturn(signInResult());

        mockMvc.perform(
                        confirm("{\"email\":\"a@b.dev\",\"code\":\"482913\"}")
                                .header(HttpHeaders.USER_AGENT, "JUnit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    @DisplayName("Không có token, cũng không đủ email và mã: 400")
    void confirm_withoutTokenOrCode_returnsValidationError() throws Exception {
        mockMvc.perform(confirm("{\"email\":\"a@b.dev\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(emailVerificationService);
    }

    @Test
    @DisplayName("Mã không đủ 6 chữ số: 400 ở trường code")
    void confirm_malformedCode_returnsFieldError() throws Exception {
        mockMvc.perform(confirm("{\"email\":\"a@b.dev\",\"code\":\"12ab\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("code"))
                .andExpect(jsonPath("$.errors[0].message").value("Mã xác thực gồm 6 chữ số"));
    }

    @Test
    @DisplayName("Mã sai hoặc hết hạn: 422 TOKEN_INVALID_OR_EXPIRED")
    void confirm_invalidCode_returnsUnprocessable() throws Exception {
        doThrow(
                        new BusinessRuleException(
                                ErrorCode.TOKEN_INVALID_OR_EXPIRED,
                                "Mã không hợp lệ hoặc đã hết hạn"))
                .when(emailVerificationService)
                .confirmByCode("a@b.dev", "000000");

        mockMvc.perform(confirm("{\"email\":\"a@b.dev\",\"code\":\"000000\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID_OR_EXPIRED"));
    }

    @Test
    @DisplayName("Gửi lại mã: luôn 202")
    void resend_validEmail_returnsAccepted() throws Exception {
        mockMvc.perform(resend("{\"email\":\"a@b.dev\"}")).andExpect(status().isAccepted());

        verify(emailVerificationService).resendVerificationEmail("a@b.dev");
    }

    @Test
    @DisplayName("Gửi lại với email sai định dạng: 400")
    void resend_invalidEmail_returnsValidationError() throws Exception {
        mockMvc.perform(resend("{\"email\":\"khong-phai-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));

        verifyNoInteractions(emailVerificationService);
    }

    private static MockHttpServletRequestBuilder confirm(String body) {
        return post("/api/auth/email-verification/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static MockHttpServletRequestBuilder resend(String body) {
        return post("/api/auth/email-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
