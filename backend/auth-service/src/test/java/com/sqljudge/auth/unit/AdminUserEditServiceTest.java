package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.dto.request.UpdateUserRequest;
import com.sqljudge.auth.dto.response.AdminUserDetailResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.repository.UserSessionRepository;
import com.sqljudge.auth.service.AdminUserEditService;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.AppException;
import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.common.security.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminUserEditServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-19T05:00:00Z");
    private static final UUID ADMIN_ID = UUID.fromString("0192f3a0-0000-7000-8000-00000000000a");
    private static final UUID TARGET_ID = UUID.fromString("0192f3a0-0000-7000-8000-00000000000b");

    @Mock private UserRepository userRepository;
    @Mock private UserSessionRepository userSessionRepository;
    @Mock private AdminActionRepository adminActionRepository;

    private AdminUserEditService service;
    private User target;

    @BeforeEach
    void setUp() {
        service =
                new AdminUserEditService(
                        userRepository,
                        userSessionRepository,
                        adminActionRepository,
                        Clock.fixed(NOW, ZoneOffset.UTC));
        target = new User();
        ReflectionTestUtils.setField(target, "id", TARGET_ID);
        target.setUsername("viet.luu");
        target.setRole(Role.LEARNER);
        target.setSignupProvider(SignupProvider.EMAIL);
        when(userRepository.findByUsername("viet.luu")).thenReturn(Optional.of(target));
    }

    @Test
    @DisplayName("Đổi vai trò: lưu vai trò mới, đăng xuất mọi phiên, ghi nhật ký cũ → mới và lý do")
    void update_roleChange_appliesRevokesAndRecords() {
        AdminUserDetailResponse updated =
                service.update("viet.luu", roleChange(Role.SETTER, " Ra đề "), ADMIN_ID);

        assertThat(updated.account().role()).isEqualTo(Role.SETTER);
        verify(userSessionRepository).revokeAllActiveSessions(target, NOW);
        AdminAction action = recordedActions().get(0);
        assertThat(action.getAction()).isEqualTo(AdminActionType.UPDATE_ROLE);
        assertThat(action.getActorId()).isEqualTo(ADMIN_ID);
        assertThat(action.getPreviousValue()).isEqualTo("LEARNER");
        assertThat(action.getNewValue()).isEqualTo("SETTER");
        assertThat(action.getReason()).isEqualTo("Ra đề");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("Đổi vai trò mà thiếu lý do: 400 ở trường reason, không đổi gì")
    void update_roleChangeWithoutReason_rejects(String reason) {
        assertThatThrownBy(
                        () -> service.update("viet.luu", roleChange(Role.SETTER, reason), ADMIN_ID))
                .isInstanceOfSatisfying(
                        InvalidFieldException.class,
                        ex -> assertThat(ex.getViolation().field()).isEqualTo("reason"));
        assertThat(target.getRole()).isEqualTo(Role.LEARNER);
        verifyNoInteractions(userSessionRepository, adminActionRepository);
    }

    @Test
    @DisplayName("Admin tự đổi vai trò của mình: 422 CANNOT_MODIFY_SELF")
    void update_ownRole_rejects() {
        assertRejected(
                () -> service.update("viet.luu", roleChange(Role.ADMIN, "Tự nâng"), TARGET_ID),
                ErrorCode.CANNOT_MODIFY_SELF);
        assertThat(target.getRole()).isEqualTo(Role.LEARNER);
    }

    @Test
    @DisplayName("Đổi vai trò của quản trị viên khác: 422 CANNOT_MODIFY_ADMIN, không đổi gì")
    void update_otherAdminRole_rejects() {
        target.setRole(Role.ADMIN);

        assertRejected(
                () -> service.update("viet.luu", roleChange(Role.LEARNER, "Hạ quyền"), ADMIN_ID),
                ErrorCode.CANNOT_MODIFY_ADMIN);
        assertThat(target.getRole()).isEqualTo(Role.ADMIN);
        verifyNoInteractions(userSessionRepository, adminActionRepository);
    }

    @Test
    @DisplayName("Đổi tên hiển thị của quản trị viên khác: vẫn được phép")
    void update_otherAdminDisplayName_changesName() {
        target.setRole(Role.ADMIN);
        UpdateUserRequest request = new UpdateUserRequest();
        request.setDisplayName("Quản trị Hà Nội");

        service.update("viet.luu", request, ADMIN_ID);

        assertThat(target.getDisplayName()).isEqualTo("Quản trị Hà Nội");
        assertThat(target.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("Gửi lại đúng vai trò hiện tại kèm tên mới: chỉ đổi tên, không cần lý do")
    void update_sameRoleWithNewName_changesNameOnly() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setRole(Role.LEARNER);
        request.setDisplayName("Huy Trần");

        service.update("viet.luu", request, TARGET_ID);

        assertThat(target.getDisplayName()).isEqualTo("Huy Trần");
        verify(userSessionRepository, never()).revokeAllActiveSessions(any(), any());
        assertThat(recordedActions())
                .extracting(AdminAction::getAction)
                .containsExactly(AdminActionType.UPDATE_DISPLAY_NAME);
    }

    @Test
    @DisplayName("Đổi tên hiển thị: bỏ khoảng trắng hai đầu, ghi nhật ký cũ → mới, không đăng xuất")
    void update_displayName_trimsAndRecords() {
        target.setDisplayName("Việt");
        UpdateUserRequest request = new UpdateUserRequest();
        request.setDisplayName("  Việt Lưu  ");

        AdminUserDetailResponse updated = service.update("viet.luu", request, ADMIN_ID);

        assertThat(updated.displayName()).isEqualTo("Việt Lưu");
        verifyNoInteractions(userSessionRepository);
        AdminAction action = recordedActions().get(0);
        assertThat(action.getAction()).isEqualTo(AdminActionType.UPDATE_DISPLAY_NAME);
        assertThat(action.getPreviousValue()).isEqualTo("Việt");
        assertThat(action.getNewValue()).isEqualTo("Việt Lưu");
        assertThat(action.getReason()).isNull();
    }

    @Test
    @DisplayName("Chỉ đổi tên mà có ghi lý do thì lưu lý do vào nhật ký")
    void update_displayNameWithReason_recordsReason() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setDisplayName("Việt Lưu");
        request.setReason(" Sửa chính tả ");

        service.update("viet.luu", request, ADMIN_ID);

        assertThat(recordedActions().get(0).getReason()).isEqualTo("Sửa chính tả");
    }

    @Test
    @DisplayName("Gửi null thì xoá tên hiển thị")
    void update_nullDisplayName_clearsName() {
        target.setDisplayName("Việt");
        UpdateUserRequest request = new UpdateUserRequest();
        request.setDisplayName(null);

        assertThat(service.update("viet.luu", request, ADMIN_ID).displayName()).isNull();
    }

    @Test
    @DisplayName("Tên không đổi thì không ghi nhật ký")
    void update_unchangedName_recordsNothing() {
        target.setDisplayName("Việt");
        UpdateUserRequest request = new UpdateUserRequest();
        request.setDisplayName("Việt");

        service.update("viet.luu", request, ADMIN_ID);

        verifyNoInteractions(adminActionRepository);
    }

    @Test
    @DisplayName("Tài khoản đã xoá hoặc không có: 404 USER_NOT_FOUND")
    void update_deletedOrMissing_rejects() {
        target.setDeletedAt(NOW.minus(Duration.ofDays(1)));
        when(userRepository.findByUsername("khong.co")).thenReturn(Optional.empty());

        assertRejected(
                () -> service.update("viet.luu", roleChange(Role.SETTER, "Ra đề"), ADMIN_ID),
                ErrorCode.USER_NOT_FOUND);
        assertRejected(
                () -> service.update("khong.co", roleChange(Role.SETTER, "Ra đề"), ADMIN_ID),
                ErrorCode.USER_NOT_FOUND);
    }

    private static UpdateUserRequest roleChange(Role role, String reason) {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setRole(role);
        request.setReason(reason);
        return request;
    }

    private List<AdminAction> recordedActions() {
        ArgumentCaptor<AdminAction> actions = ArgumentCaptor.forClass(AdminAction.class);
        verify(adminActionRepository, atLeastOnce()).save(actions.capture());
        return actions.getAllValues();
    }

    private static void assertRejected(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(
                        AppException.class, ex -> assertThat(ex.getCode()).isEqualTo(code));
    }
}
