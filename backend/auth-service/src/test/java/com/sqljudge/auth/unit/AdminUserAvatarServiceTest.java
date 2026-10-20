package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.client.AvatarStorage;
import com.sqljudge.auth.client.AvatarUpload;
import com.sqljudge.auth.client.StoredAvatar;
import com.sqljudge.auth.dto.response.UserAvatarResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.service.AdminUserAvatarService;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.AppException;
import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.common.exception.ServiceUnavailableException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionOperations;

@ExtendWith(MockitoExtension.class)
class AdminUserAvatarServiceTest {

    private static final UUID ADMIN_ID = UUID.fromString("0192f3a0-0000-7000-8000-00000000000a");
    private static final UUID USER_ID = UUID.fromString("0192f3a0-0000-7000-8000-00000000000b");
    private static final StoredAvatar NEW_AVATAR =
            new StoredAvatar("https://res.cloudinary.com/sqljudge/avatars/moi.png", "avatars/moi");
    private static final MockMultipartFile PNG_FILE =
            new MockMultipartFile("file", "anh.png", "image/png", AvatarUploadsTest.PNG);

    @Mock private UserRepository userRepository;
    @Mock private AdminActionRepository adminActionRepository;
    @Mock private AvatarStorage avatarStorage;

    private AdminUserAvatarService service;
    private User user;

    @BeforeEach
    void setUp() {
        service =
                new AdminUserAvatarService(
                        userRepository,
                        adminActionRepository,
                        avatarStorage,
                        TransactionOperations.withoutTransaction());
        user = new User();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        user.setUsername("viet.luu");
    }

    @Test
    @DisplayName("Đổi avatar: tải ảnh lên, lưu URL, ghi nhật ký, xoá ảnh cũ sau khi lưu")
    void replace_existingAvatar_storesNewAndDeletesOld() {
        user.changeAvatar("https://res.cloudinary.com/sqljudge/avatars/cu.png", "avatars/cu");
        when(userRepository.findByUsername("viet.luu")).thenReturn(Optional.of(user));
        when(avatarStorage.upload(any(AvatarUpload.class))).thenReturn(NEW_AVATAR);

        UserAvatarResponse response = service.replace("viet.luu", PNG_FILE, ADMIN_ID);

        assertThat(response.avatarUrl()).isEqualTo(NEW_AVATAR.url());
        assertThat(user.getAvatarUrl()).isEqualTo(NEW_AVATAR.url());
        assertThat(user.getAvatarPublicId()).isEqualTo("avatars/moi");
        verify(avatarStorage).delete("avatars/cu");
        ArgumentCaptor<AdminAction> saved = ArgumentCaptor.forClass(AdminAction.class);
        verify(adminActionRepository).save(saved.capture());
        assertThat(saved.getValue().getAction()).isEqualTo(AdminActionType.UPDATE_AVATAR);
        assertThat(saved.getValue().getActorId()).isEqualTo(ADMIN_ID);
        assertThat(saved.getValue().getTargetUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("Chưa có avatar: không gọi xoá ảnh cũ")
    void replace_noPreviousAvatar_deletesNothing() {
        when(userRepository.findByUsername("viet.luu")).thenReturn(Optional.of(user));
        when(avatarStorage.upload(any(AvatarUpload.class))).thenReturn(NEW_AVATAR);

        service.replace("viet.luu", PNG_FILE, ADMIN_ID);

        verify(avatarStorage, never()).delete(any());
    }

    @Test
    @DisplayName("File không phải ảnh: 400, không tải gì lên")
    void replace_invalidFile_uploadsNothing() {
        MockMultipartFile html =
                new MockMultipartFile("file", "anh.png", "image/png", "<html>".getBytes());

        assertThatThrownBy(() -> service.replace("viet.luu", html, ADMIN_ID))
                .isInstanceOf(InvalidFieldException.class);
        verifyNoInteractions(avatarStorage, adminActionRepository);
    }

    @Test
    @DisplayName("Không có tài khoản hoặc đã xoá: 404, không tải gì lên")
    void replace_missingUser_uploadsNothing() {
        when(userRepository.findByUsername("khong.co")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.replace("khong.co", PNG_FILE, ADMIN_ID))
                .isInstanceOfSatisfying(
                        AppException.class,
                        rejected ->
                                assertThat(rejected.getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
        verifyNoInteractions(avatarStorage, adminActionRepository);
    }

    @Test
    @DisplayName("Lưu DB thất bại sau khi đã tải ảnh: xoá ảnh vừa tải để không mồ côi")
    void replace_saveFails_deletesUploadedImage() {
        when(userRepository.findByUsername("viet.luu")).thenReturn(Optional.of(user));
        when(avatarStorage.upload(any(AvatarUpload.class))).thenReturn(NEW_AVATAR);
        when(adminActionRepository.save(any(AdminAction.class)))
                .thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.replace("viet.luu", PNG_FILE, ADMIN_ID))
                .isInstanceOf(IllegalStateException.class);
        verify(avatarStorage).delete("avatars/moi");
    }

    @Test
    @DisplayName("Xoá ảnh cũ thất bại: vẫn trả URL mới, chỉ ghi log")
    void replace_deleteOldFails_stillSucceeds() {
        user.changeAvatar("https://res.cloudinary.com/sqljudge/avatars/cu.png", "avatars/cu");
        when(userRepository.findByUsername("viet.luu")).thenReturn(Optional.of(user));
        when(avatarStorage.upload(any(AvatarUpload.class))).thenReturn(NEW_AVATAR);
        doThrow(new IllegalStateException("cloudinary down"))
                .when(avatarStorage)
                .delete("avatars/cu");

        assertThat(service.replace("viet.luu", PNG_FILE, ADMIN_ID).avatarUrl())
                .isEqualTo(NEW_AVATAR.url());
    }

    @Test
    @DisplayName("Cloudinary không nhận ảnh: 503 truyền nguyên, không đổi DB")
    void replace_storageUnavailable_leavesUserUnchanged() {
        when(userRepository.findByUsername("viet.luu")).thenReturn(Optional.of(user));
        when(avatarStorage.upload(any(AvatarUpload.class)))
                .thenThrow(
                        new ServiceUnavailableException(
                                ErrorCode.AVATAR_STORAGE_UNAVAILABLE, "Chưa lưu được ảnh"));

        assertThatThrownBy(() -> service.replace("viet.luu", PNG_FILE, ADMIN_ID))
                .isInstanceOf(ServiceUnavailableException.class);
        assertThat(user.getAvatarUrl()).isNull();
        verifyNoInteractions(adminActionRepository);
    }
}
