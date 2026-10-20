package com.sqljudge.auth.service;

import com.sqljudge.auth.client.AvatarStorage;
import com.sqljudge.auth.client.AvatarUpload;
import com.sqljudge.auth.client.StoredAvatar;
import com.sqljudge.auth.dto.response.UserAvatarResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserAvatarService {

    private final UserRepository userRepository;
    private final AdminActionRepository adminActionRepository;
    private final AvatarStorage avatarStorage;
    private final TransactionOperations transactionOperations;

    public UserAvatarResponse replace(String username, MultipartFile file, UUID actorId) {
        AvatarUpload upload = AvatarUploads.read(file);
        AdminUserLookup.findLive(userRepository, username);
        StoredAvatar stored = avatarStorage.upload(upload);
        String previousPublicId = attachOrDiscard(username, stored, actorId);
        deleteQuietly(previousPublicId);
        return new UserAvatarResponse(stored.url());
    }

    private String attachOrDiscard(String username, StoredAvatar stored, UUID actorId) {
        try {
            return transactionOperations.execute(status -> attach(username, stored, actorId));
        } catch (RuntimeException ex) {
            deleteQuietly(stored.publicId());
            throw ex;
        }
    }

    private String attach(String username, StoredAvatar stored, UUID actorId) {
        User user = AdminUserLookup.findLive(userRepository, username);
        String previousPublicId = user.getAvatarPublicId();
        user.changeAvatar(stored.url(), stored.publicId());
        adminActionRepository.save(AdminAction.updateAvatar(actorId, user));
        log.info("admin_user_avatar_updated userId={} actorId={}", user.getId(), actorId);
        return previousPublicId;
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
}
