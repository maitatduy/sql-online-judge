package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.dto.response.UserSessionResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.entity.UserSession;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.repository.UserSessionRepository;
import com.sqljudge.auth.service.AdminUserSessionService;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.AppException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminUserSessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-18T05:00:00Z");
    private static final UUID ADMIN_ID = UUID.fromString("0192f3a0-0000-7000-8000-00000000000a");
    private static final UUID SESSION_ID = UUID.fromString("0192f3a0-0000-7000-8000-0000000000c1");
    private static final String CHROME_ON_WINDOWS =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)"
                    + " Chrome/129.0.0.0 Safari/537.36";

    @Mock private UserRepository userRepository;
    @Mock private UserSessionRepository userSessionRepository;
    @Mock private AdminActionRepository adminActionRepository;

    private AdminUserSessionService service;
    private User user;

    @BeforeEach
    void setUp() {
        service =
                new AdminUserSessionService(
                        userRepository,
                        userSessionRepository,
                        adminActionRepository,
                        Clock.fixed(NOW, ZoneOffset.UTC));
        user = new User();
        ReflectionTestUtils.setField(
                user, "id", UUID.fromString("0192f3a0-0000-7000-8000-00000000000b"));
        user.setUsername("viet.luu");
    }

    @Test
    @DisplayName(
            "Liệt kê phiên còn hiệu lực: trình duyệt, hệ điều hành, lần dùng cuối, đang dùng trong"
                    + " 15 phút")
    void listSessions_activeSessions_describesEachSession() {
        stubUser();
        UserSession recent =
                session(SESSION_ID, CHROME_ON_WINDOWS, NOW.minus(Duration.ofMinutes(3)));
        UserSession older = session(UUID.randomUUID(), null, NOW.minus(Duration.ofDays(2)));
        when(userSessionRepository
                        .findByUserAndRevokedAtIsNullAndExpiresAtAfterOrderByLastUsedAtDesc(
                                user, NOW))
                .thenReturn(List.of(recent, older));

        List<UserSessionResponse> sessions = service.listSessions("viet.luu").items();

        assertThat(sessions).hasSize(2);
        assertThat(sessions.get(0).id()).isEqualTo(SESSION_ID);
        assertThat(sessions.get(0).browser()).isEqualTo("Chrome");
        assertThat(sessions.get(0).os()).isEqualTo("Windows");
        assertThat(sessions.get(0).lastActiveAt()).isEqualTo(NOW.minus(Duration.ofMinutes(3)));
        assertThat(sessions.get(0).isActiveNow()).isTrue();
        assertThat(sessions.get(0).city()).isNull();
        assertThat(sessions.get(0).isUnusualLocation()).isFalse();
        assertThat(sessions.get(1).browser()).isNull();
        assertThat(sessions.get(1).isActiveNow()).isFalse();
    }

    @Test
    @DisplayName("Tài khoản đã xoá vẫn xem được phiên; không có tài khoản thì 404")
    void listSessions_deletedOrMissing() {
        user.setDeletedAt(NOW.minus(Duration.ofDays(1)));
        stubUser();
        when(userRepository.findByUsername("khong.co")).thenReturn(Optional.empty());
        when(userSessionRepository
                        .findByUserAndRevokedAtIsNullAndExpiresAtAfterOrderByLastUsedAtDesc(
                                user, NOW))
                .thenReturn(List.of());

        assertThat(service.listSessions("viet.luu").items()).isEmpty();
        assertRejected(() -> service.listSessions("khong.co"), ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("Đăng xuất một phiên: thu hồi đúng phiên đó và ghi nhật ký")
    void revokeSession_activeSession_revokesAndRecords() {
        stubUser();
        UserSession target = session(SESSION_ID, CHROME_ON_WINDOWS, NOW.minus(Duration.ofHours(1)));
        when(userSessionRepository.findByIdAndUser(SESSION_ID, user))
                .thenReturn(Optional.of(target));

        service.revokeSession("viet.luu", SESSION_ID, ADMIN_ID);

        assertThat(target.getRevokedAt()).isEqualTo(NOW);
        AdminAction action = recordedAction();
        assertThat(action.getAction()).isEqualTo(AdminActionType.REVOKE_SESSION);
        assertThat(action.getActorId()).isEqualTo(ADMIN_ID);
        assertThat(action.getReason()).isNull();
    }

    @Test
    @DisplayName("Phiên không thuộc tài khoản này hoặc đã hết hiệu lực: 404 SESSION_NOT_FOUND")
    void revokeSession_unknownOrInactive_rejects() {
        stubUser();
        UUID revokedId = UUID.randomUUID();
        UserSession revoked = session(revokedId, null, NOW.minus(Duration.ofHours(1)));
        revoked.revoke(NOW.minus(Duration.ofMinutes(30)));
        when(userSessionRepository.findByIdAndUser(SESSION_ID, user)).thenReturn(Optional.empty());
        when(userSessionRepository.findByIdAndUser(revokedId, user))
                .thenReturn(Optional.of(revoked));

        assertRejected(
                () -> service.revokeSession("viet.luu", SESSION_ID, ADMIN_ID),
                ErrorCode.SESSION_NOT_FOUND);
        assertRejected(
                () -> service.revokeSession("viet.luu", revokedId, ADMIN_ID),
                ErrorCode.SESSION_NOT_FOUND);
        verifyNoInteractions(adminActionRepository);
    }

    @Test
    @DisplayName("Đăng xuất tất cả: thu hồi mọi phiên và ghi nhật ký")
    void revokeAllSessions_revokesEverySessionAndRecords() {
        stubUser();
        when(userSessionRepository.revokeAllActiveSessions(user, NOW)).thenReturn(2);

        service.revokeAllSessions("viet.luu", ADMIN_ID);

        assertThat(recordedAction().getAction()).isEqualTo(AdminActionType.REVOKE_ALL_SESSIONS);
    }

    @Test
    @DisplayName("Không còn phiên nào thì không ghi nhật ký")
    void revokeAllSessions_noActiveSession_recordsNothing() {
        stubUser();
        when(userSessionRepository.revokeAllActiveSessions(user, NOW)).thenReturn(0);

        service.revokeAllSessions("viet.luu", ADMIN_ID);

        verifyNoInteractions(adminActionRepository);
    }

    @Test
    @DisplayName("Tài khoản đã xoá thì không đăng xuất được phiên: 404 USER_NOT_FOUND")
    void revoke_deletedUser_rejects() {
        user.setDeletedAt(NOW.minus(Duration.ofDays(1)));
        stubUser();

        assertRejected(
                () -> service.revokeSession("viet.luu", SESSION_ID, ADMIN_ID),
                ErrorCode.USER_NOT_FOUND);
        assertRejected(
                () -> service.revokeAllSessions("viet.luu", ADMIN_ID), ErrorCode.USER_NOT_FOUND);
        verifyNoInteractions(userSessionRepository, adminActionRepository);
    }

    private void stubUser() {
        when(userRepository.findByUsername("viet.luu")).thenReturn(Optional.of(user));
    }

    private UserSession session(UUID id, String userAgent, Instant lastUsedAt) {
        UserSession session = new UserSession();
        ReflectionTestUtils.setField(session, "id", id);
        session.setUser(user);
        session.setUserAgent(userAgent);
        session.setLastUsedAt(lastUsedAt);
        session.setExpiresAt(NOW.plus(Duration.ofDays(7)));
        return session;
    }

    private AdminAction recordedAction() {
        ArgumentCaptor<AdminAction> action = ArgumentCaptor.forClass(AdminAction.class);
        verify(adminActionRepository).save(action.capture());
        return action.getValue();
    }

    private static void assertRejected(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(
                        AppException.class, ex -> assertThat(ex.getCode()).isEqualTo(code));
    }
}
