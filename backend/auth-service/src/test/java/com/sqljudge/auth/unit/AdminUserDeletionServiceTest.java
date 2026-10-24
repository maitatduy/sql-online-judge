package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.dto.request.DeleteUserRequest;
import com.sqljudge.auth.dto.response.AdminUserDetailResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.EmailTokenRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.repository.UserSessionRepository;
import com.sqljudge.auth.service.AdminUserDeletionService;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.AppException;
import com.sqljudge.common.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
class AdminUserDeletionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-20T03:00:00Z");
    private static final UUID ADMIN_ID = UUID.fromString("0192f3a0-0000-7000-8000-00000000000a");
    private static final UUID USER_ID = UUID.fromString("0192f3a0-0000-7000-8000-00000000000b");

    @Mock private UserRepository userRepository;
    @Mock private UserSessionRepository userSessionRepository;
    @Mock private EmailTokenRepository emailTokenRepository;
    @Mock private AdminActionRepository adminActionRepository;

    private AdminUserDeletionService service;
    private User user;

    @BeforeEach
    void setUp() {
        service =
                new AdminUserDeletionService(
                        userRepository,
                        userSessionRepository,
                        emailTokenRepository,
                        adminActionRepository,
                        Clock.fixed(NOW, ZoneOffset.UTC));
        user = new User();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        user.setUsername("viet.luu");
    }

    @Test
    @DisplayName("Xoá tài khoản: đánh dấu đã xoá, đăng xuất mọi phiên, huỷ link email, ghi nhật ký")
    void delete_liveUser_softDeletesAndRecords() {
        when(userRepository.findByUsername("viet.luu")).thenReturn(Optional.of(user));

        service.delete("viet.luu", new DeleteUserRequest("  Spam nhiều lần  "), ADMIN_ID);

        assertThat(user.getDeletedAt()).isEqualTo(NOW);
        verify(userSessionRepository).revokeAllActiveSessions(user, NOW);
        verify(emailTokenRepository).invalidateAllUnusedTokens(user, NOW);
        ArgumentCaptor<AdminAction> saved = ArgumentCaptor.forClass(AdminAction.class);
        verify(adminActionRepository).save(saved.capture());
        assertThat(saved.getValue().getAction()).isEqualTo(AdminActionType.DELETE);
        assertThat(saved.getValue().getActorId()).isEqualTo(ADMIN_ID);
        assertThat(saved.getValue().getTargetUserId()).isEqualTo(USER_ID);
        assertThat(saved.getValue().getReason()).isEqualTo("Spam nhiều lần");
    }

    @Test
    @DisplayName("Admin tự xoá chính mình: 422 CANNOT_MODIFY_SELF, không thay đổi gì")
    void delete_self_rejects() {
        when(userRepository.findByUsername("viet.luu")).thenReturn(Optional.of(user));

        assertRejected(
                () -> service.delete("viet.luu", new DeleteUserRequest("Thử"), USER_ID),
                ErrorCode.CANNOT_MODIFY_SELF);
        assertThat(user.getDeletedAt()).isNull();
        verifyNoInteractions(userSessionRepository, emailTokenRepository, adminActionRepository);
    }

    @Test
    @DisplayName("Xoá tài khoản quản trị viên khác: 422 CANNOT_MODIFY_ADMIN, không thay đổi gì")
    void delete_otherAdmin_rejects() {
        user.setRole(Role.ADMIN);
        when(userRepository.findByUsername("viet.luu")).thenReturn(Optional.of(user));

        assertRejected(
                () -> service.delete("viet.luu", new DeleteUserRequest("Thử"), ADMIN_ID),
                ErrorCode.CANNOT_MODIFY_ADMIN);
        assertThat(user.getDeletedAt()).isNull();
        verifyNoInteractions(userSessionRepository, emailTokenRepository, adminActionRepository);
    }

    @Test
    @DisplayName("Không có tài khoản hoặc đã xoá từ trước: 404 USER_NOT_FOUND")
    void delete_missingOrAlreadyDeleted_rejects() {
        User deleted = new User();
        deleted.setDeletedAt(NOW.minusSeconds(3600));
        when(userRepository.findByUsername("khong.co")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("da.xoa")).thenReturn(Optional.of(deleted));

        assertRejected(
                () -> service.delete("khong.co", new DeleteUserRequest("Lý do"), ADMIN_ID),
                ErrorCode.USER_NOT_FOUND);
        assertRejected(
                () -> service.delete("da.xoa", new DeleteUserRequest("Lý do"), ADMIN_ID),
                ErrorCode.USER_NOT_FOUND);
        verifyNoInteractions(userSessionRepository, emailTokenRepository, adminActionRepository);
    }

    @Test
    @DisplayName("Khôi phục tài khoản đã xoá: bỏ dấu xoá, ghi nhật ký, trả chi tiết")
    void restore_deletedUser_restoresAndRecords() {
        user.setDeletedAt(NOW.minusSeconds(3600));
        user.setEmail("viet.luu@gmail.com");
        user.setRole(Role.LEARNER);
        user.setSignupProvider(SignupProvider.EMAIL);
        when(userRepository.findByUsername("viet.luu")).thenReturn(Optional.of(user));

        AdminUserDetailResponse restored = service.restore("viet.luu", ADMIN_ID);

        assertThat(user.getDeletedAt()).isNull();
        assertThat(restored.account().username()).isEqualTo("viet.luu");
        assertThat(restored.account().deletedAt()).isNull();
        ArgumentCaptor<AdminAction> saved = ArgumentCaptor.forClass(AdminAction.class);
        verify(adminActionRepository).save(saved.capture());
        assertThat(saved.getValue().getAction()).isEqualTo(AdminActionType.RESTORE);
        assertThat(saved.getValue().getActorId()).isEqualTo(ADMIN_ID);
        assertThat(saved.getValue().getTargetUserId()).isEqualTo(USER_ID);
        assertThat(saved.getValue().getReason()).isNull();
    }

    @Test
    @DisplayName("Khôi phục tài khoản chưa bị xoá: 409 USER_NOT_DELETED; không có thì 404")
    void restore_liveOrMissing_rejects() {
        when(userRepository.findByUsername("viet.luu")).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("khong.co")).thenReturn(Optional.empty());

        assertRejected(() -> service.restore("viet.luu", ADMIN_ID), ErrorCode.USER_NOT_DELETED);
        assertRejected(() -> service.restore("khong.co", ADMIN_ID), ErrorCode.USER_NOT_FOUND);
        verifyNoInteractions(adminActionRepository);
    }

    private static void assertRejected(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(
                        AppException.class,
                        rejected -> assertThat(rejected.getCode()).isEqualTo(code));
    }
}
