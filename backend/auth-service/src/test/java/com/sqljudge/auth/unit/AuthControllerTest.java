package com.sqljudge.auth.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.RSAKey;
import com.sqljudge.auth.config.JwtKeyConfig;
import com.sqljudge.auth.config.SecurityConfig;
import com.sqljudge.auth.controller.AuthController;
import com.sqljudge.auth.dto.request.LoginRequest;
import com.sqljudge.auth.dto.request.RegisterRequest;
import com.sqljudge.auth.dto.response.AuthResponse;
import com.sqljudge.auth.dto.response.AuthUserResponse;
import com.sqljudge.auth.dto.response.CurrentUserResponse;
import com.sqljudge.auth.dto.response.RegisterResponse;
import com.sqljudge.auth.service.LoginService;
import com.sqljudge.auth.service.RegistrationService;
import com.sqljudge.auth.service.SignInResult;
import com.sqljudge.auth.service.UserService;
import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.common.exception.UnauthenticatedException;
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

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtKeyConfig.class})
@ImportAutoConfiguration(CommonAutoConfiguration.class)
@TestPropertySource(
        properties = {
            "sqljudge.jwt.generate-ephemeral-key=true",
            "sqljudge.security.allowed-origins=http://localhost:3000"
        })
class AuthControllerTest {

    private static final String VALID_BODY =
            """
            {"username":"minh.anh","email":"minh.anh@gmail.com",\
            "password":"matkhau-manh-2026","acceptedTerms":true}\
            """;

    @Autowired private MockMvc mockMvc;

    @Autowired private RSAKey signingKey;

    @MockitoBean private RegistrationService registrationService;
    @MockitoBean private LoginService loginService;
    @MockitoBean private UserService userService;

    @Test
    @DisplayName(
            "Khách đăng ký thành công: 201, chỉ trả email, chưa có Location vì chưa có trang hồ"
                    + " sơ, không cần CSRF token")
    void register_anonymousWithoutCsrfToken_returnsCreated() throws Exception {
        when(registrationService.register(any(RegisterRequest.class)))
                .thenReturn(new RegisterResponse("minh.anh@gmail.com"));

        mockMvc.perform(register(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION))
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

    @Test
    @DisplayName("Đăng nhập đúng: 200 kèm access token, thông tin người dùng và cookie refresh")
    void login_validCredentials_returnsTokenAndRefreshCookie() throws Exception {
        when(loginService.login(
                        new LoginRequest("minh.anh@gmail.com", "matkhau-manh-2026", true), "JUnit"))
                .thenReturn(
                        new SignInResult(
                                new AuthResponse(
                                        "access-token",
                                        new AuthUserResponse(
                                                TestJwts.USER_ID,
                                                "minh.anh",
                                                "minh.anh@gmail.com",
                                                Role.LEARNER)),
                                "refresh-token",
                                true));

        mockMvc.perform(
                        post("/api/auth/login")
                                .header(HttpHeaders.USER_AGENT, "JUnit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"email":"minh.anh@gmail.com",\
                                        "password":"matkhau-manh-2026","rememberMe":true}\
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.user.username").value("minh.anh"))
                .andExpect(jsonPath("$.user.role").value("LEARNER"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(cookie().value("sqljudge_refresh", "refresh-token"))
                .andExpect(cookie().httpOnly("sqljudge_refresh", true))
                .andExpect(cookie().path("sqljudge_refresh", "/api/auth"));
    }

    @Test
    @DisplayName("Đăng nhập thiếu dữ liệu: 400, không gọi service")
    void login_blankBody_returnsValidationError() throws Exception {
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"\",\"password\":\"\",\"rememberMe\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(loginService);
    }

    @Test
    @DisplayName("Sai thông tin: 401 INVALID_CREDENTIALS, không set cookie")
    void login_invalidCredentials_returnsUnauthorized() throws Exception {
        when(loginService.login(any(LoginRequest.class), any()))
                .thenThrow(
                        new UnauthenticatedException(
                                ErrorCode.INVALID_CREDENTIALS, "Email hoặc mật khẩu không đúng"));

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"a@b.dev\",\"password\":\"x\","
                                                + "\"rememberMe\":false}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(cookie().doesNotExist("sqljudge_refresh"));
    }

    @Test
    @DisplayName("/me khi chưa đăng nhập: 401")
    void me_anonymous_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("/me với JWT hợp lệ: trả người dùng của chính token đó")
    void me_validJwt_returnsCurrentUser() throws Exception {
        when(userService.getCurrentUser(TestJwts.USER_ID))
                .thenReturn(
                        new CurrentUserResponse(
                                TestJwts.USER_ID, "minh.anh", "Nguyễn Minh Anh", Role.LEARNER, 0));

        mockMvc.perform(
                        get("/api/auth/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + TestJwts.validToken(signingKey, "LEARNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TestJwts.USER_ID.toString()))
                .andExpect(jsonPath("$.displayName").value("Nguyễn Minh Anh"))
                .andExpect(jsonPath("$.currentStreakDays").value(0));
    }

    private static org.springframework.test.web.servlet.RequestBuilder register(String body) {
        return post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
