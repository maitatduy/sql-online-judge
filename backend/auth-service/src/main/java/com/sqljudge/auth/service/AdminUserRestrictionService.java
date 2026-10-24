package com.sqljudge.auth.service;

import com.sqljudge.auth.config.AdminProperties;
import com.sqljudge.auth.dto.request.UpdateRestrictionsRequest;
import com.sqljudge.auth.dto.response.UserRestrictionsResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.RestrictionsChange;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.entity.UserRestriction;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.repository.UserRestrictionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserRestrictionService {

    private final UserRepository userRepository;
    private final UserRestrictionRepository userRestrictionRepository;
    private final AdminActionRepository adminActionRepository;
    private final AdminProperties adminProperties;

    @Transactional(readOnly = true)
    public UserRestrictionsResponse get(String username) {
        User user = AdminUserLookup.findAny(userRepository, username);
        return toResponse(restrictionOf(user));
    }

    @Transactional
    public UserRestrictionsResponse update(
            String username, UpdateRestrictionsRequest request, UUID actorId) {
        User user = AdminUserLookup.lockLive(userRepository, username);
        AdminTargetGuard.requireModerable(user, actorId, AdminTargetGuard.Action.RESTRICT);
        UserRestriction restriction = restrictionOf(user);
        String previous = restriction.describe(defaultAiDailyQuota());
        restriction.changeTo(
                request.aiDailyQuota(), request.isBannedFromRatedContests(), defaultAiDailyQuota());
        String current = restriction.describe(defaultAiDailyQuota());
        if (previous.equals(current)) {
            return toResponse(restriction);
        }
        userRestrictionRepository.save(restriction);
        adminActionRepository.save(
                AdminAction.updateRestrictions(
                        actorId,
                        user,
                        new RestrictionsChange(previous, current, request.reason().strip())));
        log.info("admin_user_restrictions_updated userId={} actorId={}", user.getId(), actorId);
        return toResponse(restriction);
    }

    private UserRestriction restrictionOf(User user) {
        return userRestrictionRepository
                .findByUserId(user.getId())
                .orElseGet(() -> UserRestriction.defaultsFor(user));
    }

    private UserRestrictionsResponse toResponse(UserRestriction restriction) {
        return new UserRestrictionsResponse(
                restriction.effectiveAiDailyQuota(defaultAiDailyQuota()),
                restriction.isBannedFromRatedContests());
    }

    private int defaultAiDailyQuota() {
        return adminProperties.restrictions().defaultAiDailyQuota();
    }
}
