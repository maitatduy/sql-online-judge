package com.sqljudge.auth.service;

import com.sqljudge.auth.client.AvatarStorage;
import com.sqljudge.auth.client.AvatarUpload;
import com.sqljudge.auth.client.StoredAvatar;
import com.sqljudge.auth.dto.request.ChangePasswordRequest;
import com.sqljudge.auth.dto.request.UpdateAccountProfileRequest;
import com.sqljudge.auth.dto.response.AccountProfileResponse;
import com.sqljudge.auth.dto.response.UserAvatarResponse;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.entity.User.ProfileDetails;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.common.exception.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AvatarStorage avatarStorage;
    private final TransactionOperations transactionOperations;

    @Transactional(readOnly = true)
    public AccountProfileResponse profile(UUID userId) {
        return AccountProfileResponse.from(live(userId));
    }

    @Transactional
    public AccountProfileResponse updateProfile(UUID userId, UpdateAccountProfileRequest request) {
        User user = live(userId);
        user.updateProfile(
                new ProfileDetails(
                        request.displayName().trim(),
                        blankToNull(request.bio()),
                        blankToNull(request.school()),
                        blankToNull(request.location()),
                        blankToNull(request.websiteUrl())));
        log.info("account_profile_updated userId={}", userId);
        return AccountProfileResponse.from(user);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = live(userId);
        if (user.getPasswordHash() != null
                && (request.currentPassword() == null
                        || !passwordEncoder.matches(
                                request.currentPassword(), user.getPasswordHash()))) {
            throw new InvalidFieldException("currentPassword", "Mật khẩu hiện tại không đúng");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        log.info("account_password_changed userId={}", userId);
    }

    public UserAvatarResponse replaceAvatar(UUID userId, MultipartFile file) {
        AvatarUpload upload = AvatarUploads.read(file);
        StoredAvatar stored = avatarStorage.upload(upload);
        String previousPublicId;
        try {
            previousPublicId = transactionOperations.execute(status -> attach(userId, stored));
        } catch (RuntimeException ex) {
            deleteQuietly(stored.publicId());
            throw ex;
        }
        deleteQuietly(previousPublicId);
        return new UserAvatarResponse(stored.url());
    }

    public void removeAvatar(UUID userId) {
        deleteQuietly(transactionOperations.execute(status -> detach(userId)));
    }

    private String attach(UUID userId, StoredAvatar stored) {
        User user = live(userId);
        String previousPublicId = user.getAvatarPublicId();
        user.changeAvatar(stored.url(), stored.publicId());
        return previousPublicId;
    }

    private String detach(UUID userId) {
        User user = live(userId);
        String publicId = user.getAvatarPublicId();
        user.removeAvatar();
        return publicId;
    }

    private User live(UUID userId) {
        return userRepository
                .findById(userId)
                .filter(user -> !user.isDeleted())
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng"));
    }

    private void deleteQuietly(String publicId) {
        if (publicId == null) {
            return;
        }
        try {
            avatarStorage.delete(publicId);
        } catch (RuntimeException ex) {
            log.warn("avatar_delete_failed publicId={}", publicId, ex);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
