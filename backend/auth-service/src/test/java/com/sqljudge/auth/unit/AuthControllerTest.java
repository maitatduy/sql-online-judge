package com.sqljudge.auth.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sqljudge.auth.config.JwtKeyConfig;
import com.sqljudge.auth.config.SecurityConfig;
import com.sqljudge.auth.controller.AuthController;
import com.sqljudge.auth.dto.request.RegisterRequest;
import com.sqljudge.auth.dto.response.RegisterResponse;
import com.sqljudge.auth.service.RegistrationService;
import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ConflictException;
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

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtKeyConfig.class})
@ImportAutoConfiguration(CommonAutoConfiguration.class)
@TestPropertySource(properties = "sqljudge.jwt.generate-ephemeral-key=true")
class AuthControllerTest {

    private static final String VALID_BODY =
            """
            {"username":"minh.anh","email":"minh.anh@gmail.com",\
            "password":"matkhau-manh-2026","acceptedTerms":true}\
            """;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RegistrationService registrationService;

    @Test
    @DisplayName("Khách đăng ký thành công: 201, Location, chỉ trả email, không cần CSRF token")
    void register_anonymousWithoutCsrfToken_returnsCreated() throws Exception {
        when(registrationService.register(any(RegisterRequest.class)))
                .thenReturn(new RegisterResponse("minh.anh@gmail.com"));

        mockMvc.perform(register(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/users/minh.anh"))
                .andExpect(jsonPath("$.email").value("minh.anh@gmail.com"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    @DisplayName("Dữ liệu sai trả 400 kèm lỗi theo trường, không gọi service")
    void register_invalidBody_returnsFieldErrors() throws Exception {
        String body =
                """
                {"username":"me","email":"minh.anh@gmail.com",\
                "password":"12345678","acceptedTerms":false}\
                """;

        mockMvc.perform(register(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[?(@.field == 'username')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'password')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'acceptedTerms')]").exists());

        verifyNoInteractions(registrationService);
    }

    @Test
    @DisplayName("Trùng username trả 409 USERNAME_TAKEN")
    void register_takenUsername_returnsConflict() throws Exception {
        when(registrationService.register(any(RegisterRequest.class)))
                .thenThrow(
                        new ConflictException(
                                ErrorCode.USERNAME_TAKEN, "Tên người dùng này đã có người dùng"));

        mockMvc.perform(register(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_TAKEN"));
    }

    @Test
    @DisplayName("JSON hỏng trả 400 MALFORMED_REQUEST")
    void register_malformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(register("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    private static org.springframework.test.web.servlet.RequestBuilder register(String body) {
        return post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
