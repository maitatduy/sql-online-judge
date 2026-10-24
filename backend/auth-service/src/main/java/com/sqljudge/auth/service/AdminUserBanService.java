package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.request.BanUserRequest;
import com.sqljudge.auth.dto.request.UnbanUserRequest;
import com.sqljudge.auth.dto.response.AdminUserDetailResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.event.BanNotificationEmailRequestedEvent;
import com.sqljudge.auth.event.UnbanNotificationEmailRequestedEvent;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.repository.UserSessionRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ConflictException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserBanService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final AdminActionRepository adminActionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public AdminUserDetailResponse ban(String username, BanUserRequest request, UUID actorId) {
        User user = AdminUserLookup.findLive(userRepository, username);
        AdminTargetGuard.requireModerable(user, actorId, AdminTargetGuard.Action.BAN);
        Instant now = clock.instant();
        if (user.isBannedAt(now)) {
            throw new ConflictException(ErrorCode.USER_ALREADY_BANNED, "Tài khoản đã bị khoá");
        }
        applyBan(user, request, actorId);
        return AdminUserViews.toDetail(user, now);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void applyBan(User user, BanUserRequest request, UUID actorId) {
        Instant now = clock.instant();
        String reason = request.reason().strip();
        Instant until = request.duration().bannedUntilFrom(now);
        user.ban(now, until, reason);
        userSessionRepository.revokeAllActiveSessions(user, now);
        adminActionRepository.save(AdminAction.ban(actorId, user, reason));
        if (request.notifyByEmail()) {
            eventPublisher.publishEvent(
                    new BanNotificationEmailRequestedEvent(
                            user.getId(), user.getEmail(), user.getUsername(), until, reason));
        }
        log.info(
                "admin_user_banned userId={} actorId={} duration={}",
                user.getId(),
                actorId,
                request.duration());
    }

    @Transactional
    public AdminUserDetailResponse unban(String username, UnbanUserRequest request, UUID actorId) {
        User user = AdminUserLookup.findLive(userRepository, username);
        Instant now = clock.instant();
        if (!user.isBannedAt(now)) {
            throw new ConflictException(ErrorCode.USER_NOT_BANNED, "Tài khoản không bị khoá");
        }
        user.unban();
        adminActionRepository.save(AdminAction.unban(actorId, user, request.reason().strip()));
        eventPublisher.publishEvent(
                new UnbanNotificationEmailRequestedEvent(
                        user.getId(), user.getEmail(), user.getUsername()));
        log.info("admin_user_unbanned userId={} actorId={}", user.getId(), actorId);
        return AdminUserViews.toDetail(user, now);
    }
}
