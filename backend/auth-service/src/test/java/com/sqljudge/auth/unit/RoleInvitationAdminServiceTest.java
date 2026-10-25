package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.dto.response.RoleInvitationSummaryResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.entity.InvitationLink;
import com.sqljudge.auth.entity.InvitationStatus;
import com.sqljudge.auth.entity.RoleInvitation;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.event.RoleInvitationEmailRequestedEvent;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.RoleInvitationRepository;
import com.sqljudge.auth.repository.RoleInvitationRow;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.service.RoleInvitationAdminService;
import com.sqljudge.auth.service.RoleInvitationSearch;
import com.sqljudge.auth.service.SecureTokens;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.AppException;
import com.sqljudge.common.security.CurrentUser;
import com.sqljudge.common.security.Role;
import com.sqljudge.common.web.PageResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RoleInvitationAdminServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-25T05:00:00Z");
    private static final UUID INVITATION_ID =
            UUID.fromString("0192f3a0-0000-7000-8000-0000000000f1");
    private static final CurrentUser ADMIN =
            new CurrentUser(
                    UUID.fromString("0192f3a0-0000-7000-8000-00000000000a"),
                    "admin.huy",
                    Set.of(Role.ADMIN));

    @Mock private UserRepository userRepository;
    @Mock private RoleInvitationRepository roleInvitationRepository;
    @Mock private AdminActionRepository adminActionRepository;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private RoleInvitationAdminService service;

    @BeforeEach
    void setUp() {
        service =
                new RoleInvitationAdminService(
                        userRepository,
                        roleInvitationRepository,
                        adminActionRepository,
                        eventPublisher,
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Danh sách: chuyển bộ lọc trạng thái và từ khoá email, tính trạng thái từng dòng")
    void search_passesFiltersAndComputesStatus() {
        RoleInvitationRow pending = row("an@gmail.com", NOW.plus(Duration.ofDays(3)), null, null);
        RoleInvitationRow expired = row("bo@gmail.com", NOW.minusSeconds(1), null, null);
        when(roleInvitationRepository.searchInvitations(
                        eq(null), eq("%gmail\\_vn%"), eq(NOW), eq(PageRequest.of(1, 20))))
                .thenReturn(new PageImpl<>(List.of(pending, expired), PageRequest.of(1, 20), 22));

        PageResponse<RoleInvitationSummaryResponse> page =
                service.search(new RoleInvitationSearch(null, " gmail_vn "), 2, 20);

        assertThat(page.totalItems()).isEqualTo(22);
        assertThat(page.items())
                .extracting(RoleInvitationSummaryResponse::status)
                .containsExactly(InvitationStatus.PENDING, InvitationStatus.EXPIRED);
        assertThat(page.items().get(0).inviterUsername()).isEqualTo("admin.huy");
    }

    @Test
    @DisplayName("Lọc theo trạng thái gửi đúng tên trạng thái xuống truy vấn")
    void search_withStatus_passesStatusName() {
        when(roleInvitationRepository.searchInvitations(eq("REVOKED"), eq(null), eq(NOW), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.search(new RoleInvitationSearch(InvitationStatus.REVOKED, null), 1, 20);

        verify(roleInvitationRepository)
                .searchInvitations(eq("REVOKED"), eq(null), eq(NOW), eq(PageRequest.of(0, 20)));
    }

    @Test
    @DisplayName("Huỷ lời mời đang chờ: ghi thời điểm huỷ và nhật ký")
    void revoke_pending_marksRevokedAndLogs() {
        RoleInvitation invitation = invitation("an@gmail.com", NOW.plus(Duration.ofDays(3)));
        when(roleInvitationRepository.findByIdForUpdate(INVITATION_ID))
                .thenReturn(Optional.of(invitation));
        User invitee = user("an@gmail.com");
        when(userRepository.findByEmail("an@gmail.com")).thenReturn(Optional.of(invitee));

        service.revoke(INVITATION_ID, ADMIN);

        assertThat(invitation.statusAt(NOW)).isEqualTo(InvitationStatus.REVOKED);
        AdminAction action = savedAction();
        assertThat(action.getAction()).isEqualTo(AdminActionType.REVOKE_ROLE_INVITATION);
        assertThat(action.getActorId()).isEqualTo(ADMIN.id());
        assertThat(action.getTargetUserId()).isEqualTo(invitee.getId());
        assertThat(action.getNewValue()).isEqualTo("SETTER");
    }

    @Test
    @DisplayName("Không huỷ được lời mời đã nhận, đã huỷ hoặc hết hạn: 409 INVITATION_NOT_PENDING")
    void revoke_notPending_isConflict() {
        RoleInvitation expired = invitation("an@gmail.com", NOW);
        when(roleInvitationRepository.findByIdForUpdate(INVITATION_ID))
                .thenReturn(Optional.of(expired));

        assertRejected(
                () -> service.revoke(INVITATION_ID, ADMIN), ErrorCode.INVITATION_NOT_PENDING);
        verifyNoInteractions(adminActionRepository);
    }

    @Test
    @DisplayName("Lời mời không tồn tại: 404 INVITATION_NOT_FOUND")
    void revoke_unknown_isNotFound() {
        when(roleInvitationRepository.findByIdForUpdate(INVITATION_ID))
                .thenReturn(Optional.empty());

        assertRejected(() -> service.revoke(INVITATION_ID, ADMIN), ErrorCode.INVITATION_NOT_FOUND);
    }

    @Test
    @DisplayName("Gửi lại lời mời hết hạn: link mới, hạn mới 7 ngày, gửi mail, ghi nhật ký")
    void resend_expired_issuesNewLink() {
        RoleInvitation invitation = invitation("moi@gmail.com", NOW.minusSeconds(60));
        String oldHash = invitation.getTokenHash();
        when(roleInvitationRepository.findByIdForUpdate(INVITATION_ID))
                .thenReturn(Optional.of(invitation));
        when(userRepository.findByEmail("moi@gmail.com")).thenReturn(Optional.empty());

        RoleInvitationSummaryResponse response = service.resend(INVITATION_ID, ADMIN);

        assertThat(response.status()).isEqualTo(InvitationStatus.PENDING);
        assertThat(invitation.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        assertThat(invitation.getTokenHash()).isNotEqualTo(oldHash);
        ArgumentCaptor<RoleInvitationEmailRequestedEvent> mail =
                ArgumentCaptor.forClass(RoleInvitationEmailRequestedEvent.class);
        verify(eventPublisher).publishEvent(mail.capture());
        assertThat(SecureTokens.sha256(mail.getValue().token()))
                .isEqualTo(invitation.getTokenHash());
        assertThat(mail.getValue().inviterUsername()).isEqualTo("admin.huy");
        AdminAction action = savedAction();
        assertThat(action.getAction()).isEqualTo(AdminActionType.RESEND_ROLE_INVITATION);
        assertThat(action.getDetails()).isEqualTo("moi@gmail.com");
    }

    @Test
    @DisplayName("Không gửi lại được lời mời đã nhận hoặc đã huỷ")
    void resend_acceptedOrRevoked_isConflict() {
        RoleInvitation accepted = invitation("an@gmail.com", NOW.plus(Duration.ofDays(3)));
        accepted.accept(UUID.randomUUID(), NOW.minusSeconds(1));
        when(roleInvitationRepository.findByIdForUpdate(INVITATION_ID))
                .thenReturn(Optional.of(accepted));

        assertRejected(
                () -> service.resend(INVITATION_ID, ADMIN), ErrorCode.INVITATION_NOT_PENDING);
        verifyNoInteractions(eventPublisher, adminActionRepository);
    }

    private AdminAction savedAction() {
        ArgumentCaptor<AdminAction> saved = ArgumentCaptor.forClass(AdminAction.class);
        verify(adminActionRepository).save(saved.capture());
        return saved.getValue();
    }

    private static RoleInvitationRow row(
            String email, Instant expiresAt, Instant acceptedAt, Instant revokedAt) {
        return new RoleInvitationRow(
                UUID.randomUUID(),
                email,
                Role.SETTER,
                "admin.huy",
                NOW.minus(Duration.ofDays(1)),
                expiresAt,
                acceptedAt,
                revokedAt);
    }

    private static RoleInvitation invitation(String email, Instant expiresAt) {
        RoleInvitation invitation =
                RoleInvitation.of(
                        email,
                        Role.SETTER,
                        null,
                        ADMIN.id(),
                        new InvitationLink(SecureTokens.sha256("cu"), expiresAt));
        ReflectionTestUtils.setField(invitation, "id", INVITATION_ID);
        return invitation;
    }

    private static User user(String email) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setEmail(email);
        return user;
    }

    private static void assertRejected(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(
                        AppException.class, ex -> assertThat(ex.getCode()).isEqualTo(code));
    }
}
