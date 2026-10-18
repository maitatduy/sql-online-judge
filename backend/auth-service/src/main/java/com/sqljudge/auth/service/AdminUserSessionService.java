package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.UserSessionResponse;
import com.sqljudge.auth.dto.response.UserSessionsResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.entity.UserSession;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.repository.UserSessionRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
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
public class AdminUserSessionService {

    private static final String CITY_NOT_TRACKED_YET = null;
    private static final boolean LOCATION_CHECK_NOT_TRACKED_YET = false;

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final AdminActionRepository adminActionRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public UserSessionsResponse listSessions(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(this::userNotFound);
        Instant now = clock.instant();
        return new UserSessionsResponse(
                userSessionRepository
                        .findByUserAndRevokedAtIsNullAndExpiresAtAfterOrderByLastUsedAtDesc(
                                user, now)
                        .stream()
                        .map(session -> toResponse(session, now))
                        .toList());
    }

    @Transactional
    public void revokeSession(String username, UUID sessionId, UUID actorId) {
        User user = findLiveUser(username);
        Instant now = clock.instant();
        UserSession session =
                userSessionRepository
                        .findByIdAndUser(sessionId, user)
                        .filter(found -> found.isActiveAt(now))
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.SESSION_NOT_FOUND,
                                                "Không tìm thấy phiên đăng nhập"));
        session.revoke(now);
        adminActionRepository.save(AdminAction.revokeSession(actorId, user));
        log.info("admin_session_revoked userId={} actorId={}", user.getId(), actorId);
    }

    @Transactional
    public void revokeAllSessions(String username, UUID actorId) {
        User user = findLiveUser(username);
        userSessionRepository.revokeAllActiveSessions(user, clock.instant());
        adminActionRepository.save(AdminAction.revokeAllSessions(actorId, user));
        log.info("admin_all_sessions_revoked userId={} actorId={}", user.getId(), actorId);
    }

    private User findLiveUser(String username) {
        return userRepository
                .findByUsername(username)
                .filter(user -> !user.isDeleted())
                .orElseThrow(this::userNotFound);
    }

    private NotFoundException userNotFound() {
        return new NotFoundException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng");
    }

    private static UserSessionResponse toResponse(UserSession session, Instant now) {
        DeviceLabel device = DeviceLabel.of(session.getUserAgent());
        boolean isActiveNow =
                !session.getLastUsedAt().plus(AccessTokenIssuer.ACCESS_TOKEN_TTL).isBefore(now);
        return new UserSessionResponse(
                session.getId(),
                device.browser(),
                device.os(),
                CITY_NOT_TRACKED_YET,
                session.getLastUsedAt(),
                isActiveNow,
                LOCATION_CHECK_NOT_TRACKED_YET);
    }
}
