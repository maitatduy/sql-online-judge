package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.request.DeleteUserRequest;
import com.sqljudge.auth.dto.response.AdminUserDetailResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.EmailTokenRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.repository.UserSessionRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ConflictException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserDeletionService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final AdminActionRepository adminActionRepository;
    private final Clock clock;

    @Transactional
    public void delete(String username, DeleteUserRequest request, UUID actorId) {
        User user = AdminUserLookup.findLive(userRepository, username);
        AdminTargetGuard.requireModerable(user, actorId, AdminTargetGuard.Action.DELETE);
        Instant now = clock.instant();
        user.markDeleted(now);
        userSessionRepository.revokeAllActiveSessions(user, now);
        emailTokenRepository.invalidateAllUnusedTokens(user, now);
        adminActionRepository.save(AdminAction.delete(actorId, user, request.reason().strip()));
        log.info("admin_user_deleted userId={} actorId={}", user.getId(), actorId);
    }

    @Transactional
    public AdminUserDetailResponse restore(String username, UUID actorId) {
        User user = AdminUserLookup.findAny(userRepository, username);
        if (!user.isDeleted()) {
            throw new ConflictException(ErrorCode.USER_NOT_DELETED, "Tài khoản chưa bị xoá");
        }
        user.restore();
        adminActionRepository.save(AdminAction.restore(actorId, user));
        log.info("admin_user_restored userId={} actorId={}", user.getId(), actorId);
        return AdminUserViews.toDetail(user, clock.instant());
    }
}
