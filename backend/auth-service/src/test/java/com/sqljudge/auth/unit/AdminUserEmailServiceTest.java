package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.dto.response.UserEmailResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.service.AdminUserEmailService;
import com.sqljudge.auth.service.EmailVerificationService;
import com.sqljudge.auth.service.PasswordResetService;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.AppException;
import com.sqljudge.common.exception.QuotaExceededException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminUserEmailServiceTest {

    private static final UUID ADMIN_ID = UUID.fromString("0192f3a0-0000-7000-8000-00000000000a");
    private static final UUID USER_ID = UUID.fromString("0192f3a0-0000-7000-8000-00000000000b");

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetService passwordResetService;
    @Mock private EmailVerificationService emailVerificationService;
    @Mock private AdminActionRepository adminActionRepository;

    private AdminUserEmailService service;
    private User user;

    @BeforeEach
    void setUp() {
        service =
                new AdminUserEmailService(
                        userRepository,
                        passwordResetService,
                        emailVerificationService,
                        adminActionRepository);
        user = new User();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        user.setUsername("viet.luu");
    }

    @Test
    @DisplayName("Gửi link đặt lại mật khẩu: phát link cho tài khoản và ghi nhật ký")
    void sendPasswordResetEmail_liveUser_sendsAndRecords() {
        when(userRepository.findByUsername("viet.luu")).thenReturn(Optional.of(user));

        UserEmailResponse response = service.sendPasswordResetEmail("viet.luu", ADMIN_ID);

        verify(passwordResetService).sendResetLinkTo(user);
        assertThat(response.username()).isEqualTo("viet.luu");
        assertRecorded(AdminActionType.SEND_PASSWORD_RESET);
    }

    @Test
    @DisplayName("Gửi lại email xác minh: phát mã mới và ghi nhật ký")
    void sendVerificationEmail_liveUser_sendsAndRecords() {
        when(userRepository.findByUsername("viet.luu")).thenReturn(Optional.of(user));

        UserEmailResponse response = service.sendVerificationEmail("viet.luu", ADMIN_ID);

        verify(emailVerificationService).resendVerificationEmailTo(user);
        assertThat(response.username()).isEqualTo("viet.luu");
        assertRecorded(AdminActionType.SEND_VERIFICATION);
    }

    @Test
    @DisplayName("Không có tài khoản hoặc đã xoá: 404 USER_NOT_FOUND, không gửi gì")
    void sendEmails_missingOrDeleted_rejects() {
        User deleted = new User();
        deleted.setDeletedAt(Instant.parse("2026-10-01T00:00:00Z"));
        when(userRepository.findByUsername("khong.co")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("da.xoa")).thenReturn(Optional.of(deleted));

        assertNotFound(() -> service.sendPasswordResetEmail("khong.co", ADMIN_ID));
        assertNotFound(() -> service.sendVerificationEmail("da.xoa", ADMIN_ID));
        verifyNoInteractions(passwordResetService, emailVerificationService, adminActionRepository);
    }

    @Test
    @DisplayName("Đang trong thời gian chờ gửi lại: lỗi được trả nguyên, không ghi nhật ký")
    void sendPasswordResetEmail_cooldown_doesNotRecord() {
        when(userRepository.findByUsername("viet.luu")).thenReturn(Optional.of(user));
        doThrow(
                        new QuotaExceededException(
                                ErrorCode.EMAIL_RESEND_COOLDOWN,
                                "Vừa gửi email, vui lòng chờ",
                                Duration.ofSeconds(30)))
                .when(passwordResetService)
                .sendResetLinkTo(user);

        assertThatThrownBy(() -> service.sendPasswordResetEmail("viet.luu", ADMIN_ID))
                .isInstanceOf(QuotaExceededException.class);
        verifyNoInteractions(adminActionRepository);
    }

    private void assertRecorded(AdminActionType type) {
        ArgumentCaptor<AdminAction> saved = ArgumentCaptor.forClass(AdminAction.class);
        verify(adminActionRepository).save(saved.capture());
        assertThat(saved.getValue().getAction()).isEqualTo(type);
        assertThat(saved.getValue().getActorId()).isEqualTo(ADMIN_ID);
        assertThat(saved.getValue().getTargetUserId()).isEqualTo(USER_ID);
        assertThat(saved.getValue().getReason()).isNull();
    }

    private static void assertNotFound(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(
                        AppException.class,
                        rejected ->
                                assertThat(rejected.getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }
}
