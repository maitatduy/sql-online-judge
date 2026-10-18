package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.dto.request.BanDuration;
import com.sqljudge.auth.dto.request.BanUserRequest;
import com.sqljudge.auth.dto.request.UnbanUserRequest;
import com.sqljudge.auth.dto.response.AccountStatus;
import com.sqljudge.auth.dto.response.AdminUserDetailResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.event.BanNotificationEmailRequestedEvent;
import com.sqljudge.auth.event.UnbanNotificationEmailRequestedEvent;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.repository.UserSessionRepository;
import com.sqljudge.auth.service.AdminUserBanService;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.AppException;
import com.sqljudge.common.security.Role;
import java.time.Clock;
import java.time.Duration;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminUserBanServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-18T05:00:00Z");
    private static final UUID ADMIN_ID = UUID.fromString("0192f3a0-0000-7000-8000-00000000000a");
    private static final UUID TARGET_ID = UUID.fromString("0192f3a0-0000-7000-8000-00000000000b");
    private static final BanUserRequest SEVEN_DAYS_WITH_EMAIL =
            new BanUserRequest(BanDuration.DAYS_7, " Spam bình luận ", true);

    @Mock private UserRepository userRepository;
    @Mock private UserSessionRepository userSessionRepository;
    @Mock private AdminActionRepository adminActionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private AdminUserBanService service;
    private User target;

    @BeforeEach
    void setUp() {
        service =
                new AdminUserBanService(
                        userRepository,
                        userSessionRepository,
                        adminActionRepository,
                        eventPublisher,
                        Clock.fixed(NOW, ZoneOffset.UTC));
        target = new User();
        ReflectionTestUtils.setField(target, "id", TARGET_ID);
        target.setUsername("viet.luu");
        target.setEmail("viet.luu@gmail.com");
        target.setRole(Role.LEARNER);
        target.setSignupProvider(SignupProvider.EMAIL);
        target.setEmailVerifiedAt(NOW.minus(Duration.ofDays(30)));
    }

    @Test
    @DisplayName("Khoá 7 ngày: hết hạn sau 7 ngày, đăng xuất mọi phiên, ghi nhật ký, gửi mail")
    void ban_sevenDays_bansRevokesSessionsRecordsAndNotifies() {
        stubTarget();

        AdminUserDetailResponse banned = service.ban("viet.luu", SEVEN_DAYS_WITH_EMAIL, ADMIN_ID);

        assertThat(banned.account().status()).isEqualTo(AccountStatus.BANNED);
        assertThat(banned.account().bannedUntil()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        assertThat(target.getBannedAt()).isEqualTo(NOW);
        assertThat(target.getBanReason()).isEqualTo("Spam bình luận");
        verify(userSessionRepository).revokeAllActiveSessions(target, NOW);
        AdminAction action = recordedAction();
        assertThat(action.getActorId()).isEqualTo(ADMIN_ID);
        assertThat(action.getTargetUserId()).isEqualTo(TARGET_ID);
        assertThat(action.getAction()).isEqualTo(AdminActionType.BAN);
        assertThat(action.getReason()).isEqualTo("Spam bình luận");
        assertThat(action.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        ArgumentCaptor<BanNotificationEmailRequestedEvent> event =
                ArgumentCaptor.forClass(BanNotificationEmailRequestedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().email()).isEqualTo("viet.luu@gmail.com");
        assertThat(event.getValue().bannedUntil()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        assertThat(event.getValue().reason()).isEqualTo("Spam bình luận");
    }

    @Test
    @DisplayName("Khoá vĩnh viễn không có ngày hết hạn; không chọn gửi mail thì không gửi")
    void ban_permanentWithoutEmail_hasNoEndAndSendsNothing() {
        stubTarget();

        AdminUserDetailResponse banned =
                service.ban(
                        "viet.luu",
                        new BanUserRequest(BanDuration.PERMANENT, "Gian lận", false),
                        ADMIN_ID);

        assertThat(banned.account().status()).isEqualTo(AccountStatus.BANNED);
        assertThat(banned.account().bannedUntil()).isNull();
        assertThat(recordedAction().getExpiresAt()).isNull();
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Khoá được lại tài khoản có lần khoá cũ đã hết hạn")
    void ban_expiredPreviousBan_bansAgain() {
        target.setBannedAt(NOW.minus(Duration.ofDays(40)));
        target.setBannedUntil(NOW.minus(Duration.ofDays(10)));
        stubTarget();

        service.ban("viet.luu", SEVEN_DAYS_WITH_EMAIL, ADMIN_ID);

        assertThat(target.getBannedAt()).isEqualTo(NOW);
        assertThat(target.getBannedUntil()).isEqualTo(NOW.plus(Duration.ofDays(7)));
    }

    @Test
    @DisplayName("Admin tự khoá mình: 422 CANNOT_MODIFY_SELF, không đổi gì")
    void ban_self_rejects() {
        stubTarget();

        assertRejected(
                () -> service.ban("viet.luu", SEVEN_DAYS_WITH_EMAIL, TARGET_ID),
                ErrorCode.CANNOT_MODIFY_SELF);
        assertThat(target.getBannedAt()).isNull();
        verifyNoInteractions(userSessionRepository, adminActionRepository, eventPublisher);
    }

    @Test
    @DisplayName("Tài khoản đang bị khoá: 409 USER_ALREADY_BANNED")
    void ban_alreadyBanned_rejects() {
        target.setBannedAt(NOW.minus(Duration.ofDays(1)));
        stubTarget();

        assertRejected(
                () -> service.ban("viet.luu", SEVEN_DAYS_WITH_EMAIL, ADMIN_ID),
                ErrorCode.USER_ALREADY_BANNED);
        verifyNoInteractions(userSessionRepository, adminActionRepository, eventPublisher);
    }

    @Test
    @DisplayName("Không có tài khoản hoặc tài khoản đã xoá: 404 USER_NOT_FOUND")
    void ban_missingOrDeleted_rejects() {
        when(userRepository.findByUsername("khong.co")).thenReturn(Optional.empty());
        target.setDeletedAt(NOW.minus(Duration.ofDays(1)));
        stubTarget();

        assertRejected(
                () -> service.ban("khong.co", SEVEN_DAYS_WITH_EMAIL, ADMIN_ID),
                ErrorCode.USER_NOT_FOUND);
        assertRejected(
                () -> service.ban("viet.luu", SEVEN_DAYS_WITH_EMAIL, ADMIN_ID),
                ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("Mở khoá: xoá thông tin khoá, ghi nhật ký kèm lý do")
    void unban_bannedAccount_clearsBanAndRecords() {
        target.setBannedAt(NOW.minus(Duration.ofDays(1)));
        target.setBannedUntil(NOW.plus(Duration.ofDays(6)));
        target.setBanReason("Spam");
        stubTarget();

        AdminUserDetailResponse unbanned =
                service.unban("viet.luu", new UnbanUserRequest(" Khoá nhầm "), ADMIN_ID);

        assertThat(unbanned.account().status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(unbanned.account().bannedUntil()).isNull();
        assertThat(target.getBannedAt()).isNull();
        assertThat(target.getBanReason()).isNull();
        AdminAction action = recordedAction();
        assertThat(action.getAction()).isEqualTo(AdminActionType.UNBAN);
        assertThat(action.getReason()).isEqualTo("Khoá nhầm");
        assertThat(action.getExpiresAt()).isNull();
        ArgumentCaptor<UnbanNotificationEmailRequestedEvent> event =
                ArgumentCaptor.forClass(UnbanNotificationEmailRequestedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().email()).isEqualTo("viet.luu@gmail.com");
        assertThat(event.getValue().username()).isEqualTo("viet.luu");
    }

    @Test
    @DisplayName("Mở khoá tài khoản không bị khoá (kể cả khoá đã hết hạn): 409 USER_NOT_BANNED")
    void unban_notBanned_rejects() {
        target.setBannedAt(NOW.minus(Duration.ofDays(10)));
        target.setBannedUntil(NOW.minus(Duration.ofDays(3)));
        stubTarget();

        assertRejected(
                () -> service.unban("viet.luu", new UnbanUserRequest("Thử"), ADMIN_ID),
                ErrorCode.USER_NOT_BANNED);
        verify(adminActionRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    private void stubTarget() {
        when(userRepository.findByUsername("viet.luu")).thenReturn(Optional.of(target));
    }

    private AdminAction recordedAction() {
        ArgumentCaptor<AdminAction> action = ArgumentCaptor.forClass(AdminAction.class);
        verify(adminActionRepository).save(action.capture());
        return action.getValue();
    }

    private static void assertRejected(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(
                        AppException.class, ex -> assertThat(ex.getCode()).isEqualTo(code));
    }
}
