package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.dto.response.AcceptInvitationResponse;
import com.sqljudge.auth.dto.response.RoleInvitationViewResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.entity.InvitationLink;
import com.sqljudge.auth.entity.InvitationStatus;
import com.sqljudge.auth.entity.RoleInvitation;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.RoleInvitationRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.service.RoleInvitationAcceptanceService;
import com.sqljudge.auth.service.SecureTokens;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.AppException;
import com.sqljudge.common.security.CurrentUser;
import com.sqljudge.common.security.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RoleInvitationAcceptanceServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-25T05:00:00Z");
    private static final String TOKEN = "token-trong-email";
    private static final UUID INVITER_ID = UUID.fromString("0192f3a0-0000-7000-8000-00000000000a");

    @Mock private UserRepository userRepository;
    @Mock private RoleInvitationRepository roleInvitationRepository;
    @Mock private AdminActionRepository adminActionRepository;

    private RoleInvitationAcceptanceService service;
    private User invitee;

    @BeforeEach
    void setUp() {
        service =
                new RoleInvitationAcceptanceService(
                        userRepository,
                        roleInvitationRepository,
                        adminActionRepository,
                        Clock.fixed(NOW, ZoneOffset.UTC));
        invitee = user("viet.luu", "viet.luu@gmail.com", Role.LEARNER);
    }

    @Test
    @DisplayName("Xem lời mời còn hạn: che bớt email, hiện vai trò, người mời và hạn")
    void view_pending_masksEmailAndShowsInviter() {
        RoleInvitation invitation = invitation("viet.luu@gmail.com");
        when(roleInvitationRepository.findByTokenHash(SecureTokens.sha256(TOKEN)))
                .thenReturn(Optional.of(invitation));
        when(userRepository.findById(INVITER_ID))
                .thenReturn(Optional.of(user("admin.huy", "huy@sqljudge.vn", Role.ADMIN)));

        RoleInvitationViewResponse view = service.view(TOKEN);

        assertThat(view)
                .isEqualTo(
                        new RoleInvitationViewResponse(
                                "vi***@gmail.com",
                                Role.SETTER,
                                "admin.huy",
                                NOW.plus(Duration.ofDays(7)),
                                InvitationStatus.PENDING));
    }

    @Test
    @DisplayName("Phần trước @ quá ngắn vẫn được che; trạng thái hết hạn, đã huỷ, đã nhận đúng")
    void view_shortEmailAndFinishedStates() {
        RoleInvitation expired = invitation("an@x.vn");
        ReflectionTestUtils.setField(expired, "expiresAt", NOW.minusSeconds(1));
        RoleInvitation revoked = invitation("bo@x.vn");
        ReflectionTestUtils.setField(revoked, "revokedAt", NOW.minusSeconds(60));
        RoleInvitation accepted = invitation("cu@x.vn");
        accepted.accept(UUID.randomUUID(), NOW.minusSeconds(60));
        when(roleInvitationRepository.findByTokenHash(SecureTokens.sha256("a")))
                .thenReturn(Optional.of(expired));
        when(roleInvitationRepository.findByTokenHash(SecureTokens.sha256("b")))
                .thenReturn(Optional.of(revoked));
        when(roleInvitationRepository.findByTokenHash(SecureTokens.sha256("c")))
                .thenReturn(Optional.of(accepted));

        assertThat(service.view("a").maskedEmail()).isEqualTo("a***@x.vn");
        assertThat(service.view("a").status()).isEqualTo(InvitationStatus.EXPIRED);
        assertThat(service.view("b").status()).isEqualTo(InvitationStatus.REVOKED);
        assertThat(service.view("c").status()).isEqualTo(InvitationStatus.ACCEPTED);
    }

    @Test
    @DisplayName("Token không tồn tại: 404 INVITATION_NOT_FOUND")
    void view_unknownToken_isNotFound() {
        when(roleInvitationRepository.findByTokenHash(SecureTokens.sha256("sai")))
                .thenReturn(Optional.empty());

        assertRejected(() -> service.view("sai"), ErrorCode.INVITATION_NOT_FOUND);
    }

    @Test
    @DisplayName("Nhận lời mời đúng email: đổi vai trò, đánh dấu đã nhận, ghi nhật ký cũ → mới")
    void accept_matchingEmail_grantsRole() {
        RoleInvitation invitation = invitation("viet.luu@gmail.com");
        stubAccept(invitation);

        AcceptInvitationResponse response = service.accept(TOKEN, actor(invitee));

        assertThat(response.role()).isEqualTo(Role.SETTER);
        assertThat(invitee.getRole()).isEqualTo(Role.SETTER);
        assertThat(invitation.statusAt(NOW)).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(invitation.getAcceptedBy()).isEqualTo(invitee.getId());
        ArgumentCaptor<AdminAction> saved = ArgumentCaptor.forClass(AdminAction.class);
        verify(adminActionRepository).save(saved.capture());
        assertThat(saved.getValue().getAction()).isEqualTo(AdminActionType.ACCEPT_ROLE_INVITATION);
        assertThat(saved.getValue().getActorId()).isEqualTo(invitee.getId());
        assertThat(saved.getValue().getTargetUserId()).isEqualTo(invitee.getId());
        assertThat(saved.getValue().getPreviousValue()).isEqualTo("LEARNER");
        assertThat(saved.getValue().getNewValue()).isEqualTo("SETTER");
    }

    @Test
    @DisplayName("Đăng nhập bằng email khác: 403 INVITATION_EMAIL_MISMATCH, không đổi gì")
    void accept_otherEmail_isForbidden() {
        RoleInvitation invitation = invitation("nguoi.khac@gmail.com");
        stubAccept(invitation);

        assertRejected(
                () -> service.accept(TOKEN, actor(invitee)), ErrorCode.INVITATION_EMAIL_MISMATCH);
        assertThat(invitee.getRole()).isEqualTo(Role.LEARNER);
        assertThat(invitation.statusAt(NOW)).isEqualTo(InvitationStatus.PENDING);
        verifyNoInteractions(adminActionRepository);
    }

    @Test
    @DisplayName("Lời mời hết hạn, đã huỷ hoặc đã nhận thì không nhận được nữa")
    void accept_finishedInvitations_areRejected() {
        RoleInvitation expired = invitation("viet.luu@gmail.com");
        ReflectionTestUtils.setField(expired, "expiresAt", NOW);
        stubAccept(expired);
        assertRejected(() -> service.accept(TOKEN, actor(invitee)), ErrorCode.INVITATION_EXPIRED);

        RoleInvitation revoked = invitation("viet.luu@gmail.com");
        ReflectionTestUtils.setField(revoked, "revokedAt", NOW.minusSeconds(1));
        stubAccept(revoked);
        assertRejected(() -> service.accept(TOKEN, actor(invitee)), ErrorCode.INVITATION_REVOKED);

        RoleInvitation accepted = invitation("viet.luu@gmail.com");
        accepted.accept(invitee.getId(), NOW.minusSeconds(1));
        stubAccept(accepted);
        assertRejected(
                () -> service.accept(TOKEN, actor(invitee)), ErrorCode.INVITATION_ALREADY_ACCEPTED);

        assertThat(invitee.getRole()).isEqualTo(Role.LEARNER);
        verifyNoInteractions(adminActionRepository);
    }

    @Test
    @DisplayName("Tài khoản quản trị không nhận lời mời để tránh tự hạ quyền")
    void accept_byAdmin_isNotApplicable() {
        invitee.setRole(Role.ADMIN);
        stubAccept(invitation("viet.luu@gmail.com"));

        assertRejected(
                () -> service.accept(TOKEN, actor(invitee)), ErrorCode.INVITATION_NOT_APPLICABLE);
        assertThat(invitee.getRole()).isEqualTo(Role.ADMIN);
    }

    private void stubAccept(RoleInvitation invitation) {
        when(roleInvitationRepository.findByTokenHashForUpdate(SecureTokens.sha256(TOKEN)))
                .thenReturn(Optional.of(invitation));
        when(userRepository.findById(invitee.getId())).thenReturn(Optional.of(invitee));
    }

    private static RoleInvitation invitation(String email) {
        return RoleInvitation.of(
                email,
                Role.SETTER,
                null,
                INVITER_ID,
                new InvitationLink(SecureTokens.sha256(TOKEN), NOW.plus(Duration.ofDays(7))));
    }

    private static CurrentUser actor(User user) {
        return new CurrentUser(user.getId(), user.getUsername(), Set.of(user.getRole()));
    }

    private static User user(String username, String email, Role role) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    private static void assertRejected(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(
                        AppException.class, ex -> assertThat(ex.getCode()).isEqualTo(code));
    }
}
