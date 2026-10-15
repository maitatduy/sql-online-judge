package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.dto.request.LoginRequest;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.service.LoginService;
import com.sqljudge.auth.service.SignInResult;
import com.sqljudge.auth.service.SignInService;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.AppException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-15T05:00:00Z");
    private static final String EMAIL = "minh.anh@gmail.com";
    private static final String PASSWORD = "matkhau-manh-2026";
    private static final String USER_AGENT = "Mozilla/5.0";

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SignInService signInService;
    @Mock private SignInResult signInResult;

    private LoginService loginService;
    private User user;

    @BeforeEach
    void setUp() {
        loginService =
                new LoginService(
                        userRepository,
                        passwordEncoder,
                        signInService,
                        Clock.fixed(NOW, ZoneOffset.UTC));
        user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setEmail(EMAIL);
        user.setPasswordHash("hash");
        user.setEmailVerifiedAt(NOW.minusSeconds(3600));
    }

    @Test
    @DisplayName("Đúng email (khác hoa thường) và mật khẩu: đăng nhập theo lựa chọn ghi nhớ")
    void login_validCredentials_signsIn() {
        stubUser();
        when(passwordEncoder.matches(PASSWORD, "hash")).thenReturn(true);
        when(signInService.signIn(user.getId(), true, USER_AGENT)).thenReturn(signInResult);

        SignInResult result =
                loginService.login(
                        new LoginRequest(" Minh.Anh@Gmail.com ", PASSWORD, true), USER_AGENT);

        assertThat(result).isSameAs(signInResult);
    }

    @Test
    @DisplayName(
            "Email không tồn tại: 401 INVALID_CREDENTIALS, vẫn chạy BCrypt để thời gian như nhau")
    void login_unknownEmail_rejectsAfterHashing() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertRejected(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS);
        verify(passwordEncoder).matches(eq(PASSWORD), any());
    }

    @Test
    @DisplayName("Sai mật khẩu: 401 INVALID_CREDENTIALS")
    void login_wrongPassword_rejects() {
        stubUser();
        when(passwordEncoder.matches(PASSWORD, "hash")).thenReturn(false);

        assertRejected(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("Tài khoản chỉ đăng nhập bằng OAuth (không có mật khẩu): 401")
    void login_userWithoutPassword_rejects() {
        user.setPasswordHash(null);
        stubUser();

        assertRejected(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("Tài khoản đã xoá: 401 như sai thông tin, không lộ trạng thái")
    void login_deletedUser_rejectsAsInvalidCredentials() {
        user.setDeletedAt(NOW.minusSeconds(60));
        stubUser();
        when(passwordEncoder.matches(PASSWORD, "hash")).thenReturn(true);

        assertRejected(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("Đúng mật khẩu nhưng chưa xác thực email: 403 EMAIL_NOT_VERIFIED")
    void login_unverifiedEmail_rejects() {
        user.setEmailVerifiedAt(null);
        stubUser();
        when(passwordEncoder.matches(PASSWORD, "hash")).thenReturn(true);

        assertRejected(HttpStatus.FORBIDDEN, ErrorCode.EMAIL_NOT_VERIFIED);
    }

    @Test
    @DisplayName("Đang bị khoá (có hạn hoặc vĩnh viễn): 403 ACCOUNT_BANNED")
    void login_bannedUser_rejects() {
        user.setBannedAt(NOW.minusSeconds(60));
        user.setBannedUntil(NOW.plusSeconds(3600));
        stubUser();
        when(passwordEncoder.matches(PASSWORD, "hash")).thenReturn(true);

        assertRejected(HttpStatus.FORBIDDEN, ErrorCode.ACCOUNT_BANNED);

        user.setBannedUntil(null);
        assertRejected(HttpStatus.FORBIDDEN, ErrorCode.ACCOUNT_BANNED);
    }

    @Test
    @DisplayName("Hạn khoá đã qua thì đăng nhập được")
    void login_expiredBan_signsIn() {
        user.setBannedAt(NOW.minusSeconds(7200));
        user.setBannedUntil(NOW.minusSeconds(1));
        stubUser();
        when(passwordEncoder.matches(PASSWORD, "hash")).thenReturn(true);
        when(signInService.signIn(user.getId(), false, USER_AGENT)).thenReturn(signInResult);

        assertThat(loginService.login(new LoginRequest(EMAIL, PASSWORD, false), USER_AGENT))
                .isSameAs(signInResult);
    }

    private void stubUser() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    private void assertRejected(HttpStatus status, ErrorCode code) {
        assertThatThrownBy(
                        () ->
                                loginService.login(
                                        new LoginRequest(EMAIL, PASSWORD, false), USER_AGENT))
                .isInstanceOf(AppException.class)
                .satisfies(
                        exception -> {
                            AppException appException = (AppException) exception;
                            assertThat(appException.getStatus()).isEqualTo(status);
                            assertThat(appException.getCode()).isEqualTo(code);
                        });
        verify(signInService, never()).signIn(any(), anyBoolean(), anyString());
    }
}
