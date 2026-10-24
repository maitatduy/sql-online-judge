package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.config.AdminProperties;
import com.sqljudge.auth.dto.request.UpdateRestrictionsRequest;
import com.sqljudge.auth.dto.response.UserRestrictionsResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.entity.UserRestriction;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.repository.UserRestrictionRepository;
import com.sqljudge.auth.service.AdminUserRestrictionService;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.AppException;
import com.sqljudge.common.security.Role;
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
class AdminUserRestrictionServiceTest {

    private static final UUID ADMIN_ID = UUID.fromString("0192f3a0-0000-7000-8000-00000000000a");
    private static final UUID TARGET_ID = UUID.fromString("0192f3a0-0000-7000-8000-00000000000b");
    private static final int DEFAULT_QUOTA = 20;

    @Mock private UserRepository userRepository;
    @Mock private UserRestrictionRepository userRestrictionRepository;
    @Mock private AdminActionRepository adminActionRepository;

    private AdminUserRestrictionService service;
    private User target;

    @BeforeEach
    void setUp() {
        service = serviceWithDefaultQuota(DEFAULT_QUOTA);
        target = new User();
        ReflectionTestUtils.setField(target, "id", TARGET_ID);
        target.setUsername("viet.luu");
    }

    @Test
    @DisplayName("Chưa từng đặt hạn chế: trả mặc định, kể cả tài khoản đã xoá")
    void get_neverSet_returnsDefaults() {
        target.markDeleted(Instant.parse("2026-10-21T03:00:00Z"));
        when(userRepository.findByUsername("viet.luu")).thenReturn(Optional.of(target));
        when(userRestrictionRepository.findByUserId(TARGET_ID)).thenReturn(Optional.empty());

        assertThat(service.get("viet.luu"))
                .isEqualTo(new UserRestrictionsResponse(DEFAULT_QUOTA, false));
    }

    @Test
    @DisplayName("Đã đặt hạn chế: trả đúng giá trị đã lưu")
    void get_stored_returnsStoredValues() {
        UserRestriction stored = UserRestriction.defaultsFor(target);
        stored.changeTo(5, true, DEFAULT_QUOTA);
        when(userRepository.findByUsername("viet.luu")).thenReturn(Optional.of(target));
        when(userRestrictionRepository.findByUserId(TARGET_ID)).thenReturn(Optional.of(stored));

        assertThat(service.get("viet.luu")).isEqualTo(new UserRestrictionsResponse(5, true));
    }

    @Test
    @DisplayName(
            "Đổi hạn mức mặc định trong cấu hình: tài khoản chưa từng chỉnh hạn mức nhận giá trị"
                    + " mới, tài khoản đã chỉnh giữ nguyên")
    void get_defaultChanged_followsNewDefaultOnlyWhenNeverSet() {
        UserRestriction ratedBanOnly = UserRestriction.defaultsFor(target);
        ratedBanOnly.changeTo(null, true, DEFAULT_QUOTA);
        when(userRepository.findByUsername("viet.luu")).thenReturn(Optional.of(target));
        when(userRestrictionRepository.findByUserId(TARGET_ID))
                .thenReturn(Optional.of(ratedBanOnly));

        assertThat(serviceWithDefaultQuota(30).get("viet.luu"))
                .isEqualTo(new UserRestrictionsResponse(30, true));

        ratedBanOnly.changeTo(5, null, DEFAULT_QUOTA);
        assertThat(serviceWithDefaultQuota(30).get("viet.luu"))
                .isEqualTo(new UserRestrictionsResponse(5, true));
    }

    @Test
    @DisplayName(
            "Đặt hạn chế lần đầu: khoá dòng tài khoản, tạo dòng mới, chỉ đổi trường được gửi, ghi"
                    + " nhật ký giá trị trước và sau kèm lý do")
    void update_firstTime_createsAndAudits() {
        when(userRepository.findByUsernameForUpdate("viet.luu")).thenReturn(Optional.of(target));
        when(userRestrictionRepository.findByUserId(TARGET_ID)).thenReturn(Optional.empty());
        when(userRestrictionRepository.save(any(UserRestriction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserRestrictionsResponse response =
                service.update(
                        "viet.luu",
                        new UpdateRestrictionsRequest(5, null, " Lạm dụng AI "),
                        ADMIN_ID);

        assertThat(response).isEqualTo(new UserRestrictionsResponse(5, false));
        ArgumentCaptor<UserRestriction> saved = ArgumentCaptor.forClass(UserRestriction.class);
        verify(userRestrictionRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(TARGET_ID);
        ArgumentCaptor<AdminAction> action = ArgumentCaptor.forClass(AdminAction.class);
        verify(adminActionRepository).save(action.capture());
        assertThat(action.getValue().getAction()).isEqualTo(AdminActionType.UPDATE_RESTRICTIONS);
        assertThat(action.getValue().getActorId()).isEqualTo(ADMIN_ID);
        assertThat(action.getValue().getTargetUserId()).isEqualTo(TARGET_ID);
        assertThat(action.getValue().getPreviousValue())
                .isEqualTo("aiDailyQuota=default(20);isBannedFromRatedContests=false");
        assertThat(action.getValue().getNewValue())
                .isEqualTo("aiDailyQuota=5;isBannedFromRatedContests=false");
        assertThat(action.getValue().getReason()).isEqualTo("Lạm dụng AI");
    }

    @Test
    @DisplayName(
            "Gửi đúng giá trị đang có (kể cả hạn mức bằng mặc định): không đổi gì, vẫn theo mặc"
                    + " định, không ghi nhật ký")
    void update_sameValues_recordsNothing() {
        UserRestriction stored = UserRestriction.defaultsFor(target);
        when(userRepository.findByUsernameForUpdate("viet.luu")).thenReturn(Optional.of(target));
        when(userRestrictionRepository.findByUserId(TARGET_ID)).thenReturn(Optional.of(stored));

        UserRestrictionsResponse response =
                service.update(
                        "viet.luu", new UpdateRestrictionsRequest(20, false, "Kiểm tra"), ADMIN_ID);

        assertThat(response).isEqualTo(new UserRestrictionsResponse(DEFAULT_QUOTA, false));
        assertThat(stored.getAiDailyQuota()).isNull();
        verify(userRestrictionRepository, never()).save(any());
        verifyNoInteractions(adminActionRepository);
    }

    @Test
    @DisplayName("Admin tự đặt hạn chế cho chính mình: 422 CANNOT_MODIFY_SELF")
    void update_self_isRejected() {
        ReflectionTestUtils.setField(target, "id", ADMIN_ID);
        when(userRepository.findByUsernameForUpdate("viet.luu")).thenReturn(Optional.of(target));

        assertThatThrownBy(
                        () ->
                                service.update(
                                        "viet.luu",
                                        new UpdateRestrictionsRequest(null, true, "Thử"),
                                        ADMIN_ID))
                .isInstanceOfSatisfying(
                        AppException.class,
                        rejected -> {
                            assertThat(rejected.getCode()).isEqualTo(ErrorCode.CANNOT_MODIFY_SELF);
                            assertThat(rejected.getMessage())
                                    .isEqualTo("Bạn không thể tự đặt hạn chế cho chính mình");
                        });
        verifyNoInteractions(userRestrictionRepository, adminActionRepository);
    }

    @Test
    @DisplayName("Đặt hạn chế cho quản trị viên khác: 422 CANNOT_MODIFY_ADMIN")
    void update_otherAdmin_isRejected() {
        target.setRole(Role.ADMIN);
        when(userRepository.findByUsernameForUpdate("viet.luu")).thenReturn(Optional.of(target));

        assertThatThrownBy(
                        () ->
                                service.update(
                                        "viet.luu",
                                        new UpdateRestrictionsRequest(null, true, "Thử"),
                                        ADMIN_ID))
                .isInstanceOfSatisfying(
                        AppException.class,
                        rejected -> {
                            assertThat(rejected.getCode()).isEqualTo(ErrorCode.CANNOT_MODIFY_ADMIN);
                            assertThat(rejected.getMessage())
                                    .isEqualTo("Không thể đặt hạn chế cho quản trị viên khác");
                        });
        verifyNoInteractions(userRestrictionRepository, adminActionRepository);
    }

    @Test
    @DisplayName("Đặt hạn chế cho tài khoản đã xoá: 404 USER_NOT_FOUND")
    void update_deletedUser_throwsNotFound() {
        target.markDeleted(Instant.parse("2026-10-21T03:00:00Z"));
        when(userRepository.findByUsernameForUpdate("viet.luu")).thenReturn(Optional.of(target));

        assertThatThrownBy(
                        () ->
                                service.update(
                                        "viet.luu",
                                        new UpdateRestrictionsRequest(null, true, "Thử"),
                                        ADMIN_ID))
                .isInstanceOfSatisfying(
                        AppException.class,
                        rejected ->
                                assertThat(rejected.getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
        verifyNoInteractions(userRestrictionRepository, adminActionRepository);
    }

    private AdminUserRestrictionService serviceWithDefaultQuota(int defaultQuota) {
        return new AdminUserRestrictionService(
                userRepository,
                userRestrictionRepository,
                adminActionRepository,
                new AdminProperties(
                        new AdminProperties.UserExport(500),
                        new AdminProperties.Restrictions(defaultQuota)));
    }
}
