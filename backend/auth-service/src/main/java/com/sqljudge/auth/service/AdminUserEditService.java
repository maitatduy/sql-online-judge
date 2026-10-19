package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.request.UpdateUserRequest;
import com.sqljudge.auth.dto.response.AdminUserDetailResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.entity.ValueChange;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.repository.UserSessionRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.common.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserEditService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final AdminActionRepository adminActionRepository;
    private final Clock clock;

    @Transactional
    public AdminUserDetailResponse update(
            String username, UpdateUserRequest request, UUID actorId) {
        User user = AdminUserLookup.findLive(userRepository, username);
        boolean isRoleChanged = request.isRoleProvided() && request.getRole() != user.getRole();
        if (isRoleChanged) {
            requireRoleChangeAllowed(user, request, actorId);
        }
        if (request.isDisplayNameProvided()) {
            changeDisplayName(user, request, actorId);
        }
        if (isRoleChanged) {
            changeRole(user, request, actorId);
        }
        return AdminUserViews.toDetail(user, clock.instant());
    }

    private void requireRoleChangeAllowed(User user, UpdateUserRequest request, UUID actorId) {
        if (user.getId().equals(actorId)) {
            throw new BusinessRuleException(
                    ErrorCode.CANNOT_MODIFY_SELF, "Bạn không thể tự đổi vai trò của chính mình");
        }
        if (reasonOf(request) == null) {
            throw new InvalidFieldException("reason", "Không được để trống");
        }
    }

    private void changeDisplayName(User user, UpdateUserRequest request, UUID actorId) {
        String newName = request.getDisplayName() == null ? null : request.getDisplayName().strip();
        String previousName = user.getDisplayName();
        if (Objects.equals(previousName, newName)) {
            return;
        }
        user.setDisplayName(newName);
        adminActionRepository.save(
                AdminAction.changeDisplayName(
                        actorId, user, new ValueChange(previousName, reasonOf(request))));
    }

    private void changeRole(User user, UpdateUserRequest request, UUID actorId) {
        Role previousRole = user.getRole();
        user.setRole(request.getRole());
        Instant now = clock.instant();
        userSessionRepository.revokeAllActiveSessions(user, now);
        adminActionRepository.save(
                AdminAction.changeRole(
                        actorId, user, new ValueChange(previousRole.name(), reasonOf(request))));
        log.info(
                "admin_user_role_changed userId={} actorId={} from={} to={}",
                user.getId(),
                actorId,
                previousRole,
                user.getRole());
    }

    private static String reasonOf(UpdateUserRequest request) {
        String reason = request.getReason();
        return reason == null || reason.isBlank() ? null : reason.strip();
    }
}
