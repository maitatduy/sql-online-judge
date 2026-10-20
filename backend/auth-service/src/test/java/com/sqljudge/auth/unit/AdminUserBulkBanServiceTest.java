package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.dto.request.BanDuration;
import com.sqljudge.auth.dto.request.BulkBanUsersRequest;
import com.sqljudge.auth.dto.response.BulkUserActionResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.event.BanNotificationEmailRequestedEvent;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.repository.UserSessionRepository;
import com.sqljudge.auth.service.AdminUserBanService;
import com.sqljudge.auth.service.AdminUserBulkService;
import com.sqljudge.auth.service.AdminUserEditService;
import com.sqljudge.common.security.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
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
class AdminUserBulkBanServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-20T05:00:00Z");
    private static final UUID ADMIN_ID = UUID.fromString("0192f3a0-0000-7000-8000-00000000000a");

    @Mock private UserRepository userRepository;
    @Mock private UserSessionRepository userSessionRepository;
    @Mock private AdminActionRepository adminActionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private AdminUserBulkService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service =
                new AdminUserBulkService(
                        userRepository,
                        new AdminUserBanService(
                                userRepository,
                                userSessionRepository,
                                adminActionRepository,
                                eventPublisher,
                                clock),
                        new AdminUserEditService(
                                userRepository,
                                userSessionRepository,
                                adminActionRepository,
                                clock),
                        clock);
    }

    @Test
    @DisplayName(
            "Khoá hàng loạt: khoá như khoá đơn lẻ (thu hồi phiên, nhật ký, email), bỏ qua chính"
                    + " mình, admin, đã bị khoá, đã xoá, không tồn tại")
    void ban_mixedTargets_bansOnlyEligible() {
        User learner = AdminUserBulkRoleServiceTest.user("an.bui", Role.LEARNER);
        User setter = AdminUserBulkRoleServiceTest.user("binh.setter", Role.SETTER);
        User self = AdminUserBulkRoleServiceTest.user("admin.me", Role.MODERATOR);
        ReflectionTestUtils.setField(self, "id", ADMIN_ID);
        User otherAdmin = AdminUserBulkRoleServiceTest.user("admin.khac", Role.ADMIN);
        User banned = AdminUserBulkRoleServiceTest.user("da.khoa", Role.LEARNER);
        banned.ban(NOW.minus(Duration.ofDays(1)), null, "Cũ");
        User deleted = AdminUserBulkRoleServiceTest.user("da.xoa", Role.LEARNER);
        deleted.markDeleted(NOW);
        when(userRepository.findAllByUsernameIn(any()))
                .thenReturn(List.of(learner, setter, self, otherAdmin, banned, deleted));

        BulkUserActionResponse response =
                service.ban(
                        new BulkBanUsersRequest(
                                List.of(
                                        "an.bui",
                                        "binh.setter",
                                        "admin.me",
                                        "admin.khac",
                                        "da.khoa",
                                        "da.xoa",
                                        "khong.co"),
                                BanDuration.DAYS_7,
                                " Spam hàng loạt ",
                                true),
                        ADMIN_ID);

        assertThat(response.affectedCount()).isEqualTo(2);
        assertThat(response.skippedUsernames())
                .containsExactly("admin.me", "admin.khac", "da.khoa", "da.xoa", "khong.co");
        Instant until = NOW.plus(Duration.ofDays(7));
        assertThat(learner.isBannedAt(NOW)).isTrue();
        assertThat(learner.getBannedUntil()).isEqualTo(until);
        assertThat(learner.getBanReason()).isEqualTo("Spam hàng loạt");
        assertThat(otherAdmin.isBannedAt(NOW)).isFalse();
        assertThat(banned.getBanReason()).isEqualTo("Cũ");
        verify(userSessionRepository).revokeAllActiveSessions(learner, NOW);
        verify(userSessionRepository).revokeAllActiveSessions(setter, NOW);
        verify(userSessionRepository, never()).revokeAllActiveSessions(otherAdmin, NOW);
        ArgumentCaptor<AdminAction> saved = ArgumentCaptor.forClass(AdminAction.class);
        verify(adminActionRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues())
                .extracting(AdminAction::getAction, AdminAction::getTargetUserId)
                .containsExactly(
                        tuple(AdminActionType.BAN, learner.getId()),
                        tuple(AdminActionType.BAN, setter.getId()));
        verify(eventPublisher, times(2))
                .publishEvent(any(BanNotificationEmailRequestedEvent.class));
    }

    @Test
    @DisplayName("Khoá hàng loạt không chọn gửi email: không phát yêu cầu gửi email nào")
    void ban_withoutEmail_publishesNothing() {
        User learner = AdminUserBulkRoleServiceTest.user("an.bui", Role.LEARNER);
        when(userRepository.findAllByUsernameIn(any())).thenReturn(List.of(learner));

        BulkUserActionResponse response =
                service.ban(
                        new BulkBanUsersRequest(
                                List.of("an.bui"), BanDuration.PERMANENT, "Gian lận", false),
                        ADMIN_ID);

        assertThat(response.affectedCount()).isEqualTo(1);
        assertThat(learner.getBannedUntil()).isNull();
        verifyNoInteractions(eventPublisher);
    }
}
