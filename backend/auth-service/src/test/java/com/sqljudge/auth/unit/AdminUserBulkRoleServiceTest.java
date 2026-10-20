package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.dto.request.BulkChangeRoleRequest;
import com.sqljudge.auth.dto.response.BulkUserActionResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.repository.UserSessionRepository;
import com.sqljudge.auth.service.AdminUserBanService;
import com.sqljudge.auth.service.AdminUserBulkService;
import com.sqljudge.auth.service.AdminUserEditService;
import com.sqljudge.common.security.Role;
import java.time.Clock;
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
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class AdminUserBulkRoleServiceTest {

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
            "Đổi vai trò hàng loạt: chỉ đổi tài khoản hợp lệ, thu hồi phiên, ghi nhật ký, bỏ qua"
                    + " chính mình, admin, đã đúng vai trò, đã xoá, không tồn tại")
    void changeRole_mixedTargets_appliesOnlyEligible(CapturedOutput output) {
        User learner = user("an.bui", Role.LEARNER);
        User self = user("admin.me", Role.ADMIN);
        ReflectionTestUtils.setField(self, "id", ADMIN_ID);
        User otherAdmin = user("admin.khac", Role.ADMIN);
        User alreadySetter = user("da.setter", Role.SETTER);
        User deleted = user("da.xoa", Role.LEARNER);
        deleted.markDeleted(NOW);
        when(userRepository.findAllByUsernameIn(any()))
                .thenReturn(List.of(learner, self, otherAdmin, alreadySetter, deleted));

        BulkUserActionResponse response =
                service.changeRole(
                        new BulkChangeRoleRequest(
                                List.of(
                                        "an.bui",
                                        "admin.me",
                                        "admin.khac",
                                        "da.setter",
                                        "da.xoa",
                                        "khong.co"),
                                Role.SETTER,
                                " Cộng tác ra đề "),
                        ADMIN_ID);

        assertThat(response.affectedCount()).isEqualTo(1);
        assertThat(response.skippedUsernames())
                .containsExactly("admin.me", "admin.khac", "da.setter", "da.xoa", "khong.co");
        assertThat(learner.getRole()).isEqualTo(Role.SETTER);
        assertThat(otherAdmin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(deleted.getRole()).isEqualTo(Role.LEARNER);
        verify(userSessionRepository).revokeAllActiveSessions(learner, NOW);
        verify(userSessionRepository, never()).revokeAllActiveSessions(otherAdmin, NOW);
        ArgumentCaptor<AdminAction> saved = ArgumentCaptor.forClass(AdminAction.class);
        verify(adminActionRepository).save(saved.capture());
        AdminAction action = saved.getValue();
        assertThat(action.getAction()).isEqualTo(AdminActionType.UPDATE_ROLE);
        assertThat(action.getActorId()).isEqualTo(ADMIN_ID);
        assertThat(action.getTargetUserId()).isEqualTo(learner.getId());
        assertThat(action.getPreviousValue()).isEqualTo("LEARNER");
        assertThat(action.getNewValue()).isEqualTo("SETTER");
        assertThat(action.getReason()).isEqualTo("Cộng tác ra đề");
        assertThat(output)
                .contains("admin_user_role_changed userId=" + learner.getId())
                .doesNotContain("admin_user_role_changed userId=" + otherAdmin.getId());
    }

    @Test
    @DisplayName("Username lặp lại trong danh sách chỉ được đổi và đếm một lần")
    void changeRole_duplicateUsernames_countedOnce() {
        User learner = user("an.bui", Role.LEARNER);
        when(userRepository.findAllByUsernameIn(any())).thenReturn(List.of(learner));

        BulkUserActionResponse response =
                service.changeRole(
                        new BulkChangeRoleRequest(
                                List.of("an.bui", "an.bui"), Role.MODERATOR, "Kiểm duyệt"),
                        ADMIN_ID);

        assertThat(response.affectedCount()).isEqualTo(1);
        assertThat(response.skippedUsernames()).isEmpty();
        verify(adminActionRepository).save(any(AdminAction.class));
    }

    static User user(String username, Role role) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(username + "@gmail.com");
        user.setRole(role);
        return user;
    }
}
