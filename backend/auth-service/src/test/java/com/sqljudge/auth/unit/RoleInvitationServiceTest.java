package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.dto.request.InviteRolesRequest;
import com.sqljudge.auth.dto.response.InviteRolesResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.entity.RoleInvitation;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.event.RoleInvitationEmailRequestedEvent;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.RoleInvitationRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.service.RoleInvitationService;
import com.sqljudge.auth.service.SecureTokens;
import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.common.security.CurrentUser;
import com.sqljudge.common.security.Role;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RoleInvitationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-25T05:00:00Z");
    private static final CurrentUser ADMIN =
            new CurrentUser(
                    UUID.fromString("0192f3a0-0000-7000-8000-00000000000a"),
                    "admin.huy",
                    Set.of(Role.ADMIN));

    @Mock private UserRepository userRepository;
    @Mock private RoleInvitationRepository roleInvitationRepository;
    @Mock private AdminActionRepository adminActionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private RoleInvitationService service;

    @BeforeEach
    void setUp() {
        service =
                new RoleInvitationService(
                        userRepository,
                        roleInvitationRepository,
                        adminActionRepository,
                        eventPublisher,
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Mời một học viên: lưu lời mời 7 ngày với token đã băm, gửi mail, ghi nhật ký")
    void invite_learner_createsInvitationEmailAndAuditEntry() {
        User learner = user("viet.luu", "viet.luu@gmail.com", Role.LEARNER);
        when(userRepository.findByEmail("viet.luu@gmail.com")).thenReturn(Optional.of(learner));

        InviteRolesResponse response =
                service.invite(
                        new InviteRolesRequest(
                                List.of(" Viet.Luu@Gmail.com "), Role.SETTER, " Ra đề Weekly "),
                        ADMIN);

        assertThat(response.sentCount()).isEqualTo(1);
        assertThat(response.skipped()).isEmpty();
        RoleInvitation invitation = savedInvitation();
        assertThat(invitation.getEmail()).isEqualTo("viet.luu@gmail.com");
        assertThat(invitation.getRole()).isEqualTo(Role.SETTER);
        assertThat(invitation.getMessage()).isEqualTo("Ra đề Weekly");
        assertThat(invitation.getInvitedBy()).isEqualTo(ADMIN.id());
        assertThat(invitation.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        RoleInvitationEmailRequestedEvent mail = publishedMail();
        assertThat(mail.email()).isEqualTo("viet.luu@gmail.com");
        assertThat(mail.inviterUsername()).isEqualTo("admin.huy");
        assertThat(SecureTokens.sha256(mail.token())).isEqualTo(invitation.getTokenHash());
        assertThat(mail.toString()).doesNotContain(mail.token());
        AdminAction action = savedAction();
        assertThat(action.getAction()).isEqualTo(AdminActionType.INVITE_ROLE);
        assertThat(action.getTargetUserId()).isEqualTo(learner.getId());
        assertThat(action.getNewValue()).isEqualTo("SETTER");
        assertThat(action.getDetails()).isNull();
    }

    @Test
    @DisplayName("Email chưa có tài khoản vẫn được mời; nhật ký ghi email vì chưa có đối tượng")
    void invite_unknownEmail_isInvitedAndLoggedByEmail() {
        when(userRepository.findByEmail("moi@gmail.com")).thenReturn(Optional.empty());

        InviteRolesResponse response =
                service.invite(
                        new InviteRolesRequest(List.of("moi@gmail.com"), Role.MODERATOR, null),
                        ADMIN);

        assertThat(response.sentCount()).isEqualTo(1);
        assertThat(savedInvitation().getMessage()).isNull();
        AdminAction action = savedAction();
        assertThat(action.getTargetUserId()).isNull();
        assertThat(action.getDetails()).isEqualTo("moi@gmail.com");
    }

    @Test
    @DisplayName("Bỏ qua kèm lý do: đã có vai trò, là quản trị, bị khoá, đã xoá, đang chờ lời mời")
    void invite_ineligibleRecipients_areSkippedWithReasons() {
        when(userRepository.findByEmail("setter@gmail.com"))
                .thenReturn(Optional.of(user("setter", "setter@gmail.com", Role.SETTER)));
        when(userRepository.findByEmail("admin@gmail.com"))
                .thenReturn(Optional.of(user("admin", "admin@gmail.com", Role.ADMIN)));
        User banned = user("bi.khoa", "bi.khoa@gmail.com", Role.LEARNER);
        banned.ban(NOW.minusSeconds(60), null, "Spam");
        when(userRepository.findByEmail("bi.khoa@gmail.com")).thenReturn(Optional.of(banned));
        User deleted = user("da.xoa", "da.xoa@gmail.com", Role.LEARNER);
        deleted.markDeleted(NOW.minusSeconds(60));
        when(userRepository.findByEmail("da.xoa@gmail.com")).thenReturn(Optional.of(deleted));
        when(userRepository.findByEmail("dang.cho@gmail.com")).thenReturn(Optional.empty());
        when(roleInvitationRepository.hasPendingInvitation("dang.cho@gmail.com", NOW))
                .thenReturn(true);

        InviteRolesResponse response =
                service.invite(
                        new InviteRolesRequest(
                                List.of(
                                        "setter@gmail.com",
                                        "admin@gmail.com",
                                        "bi.khoa@gmail.com",
                                        "da.xoa@gmail.com",
                                        "dang.cho@gmail.com"),
                                Role.SETTER,
                                null),
                        ADMIN);

        assertThat(response.sentCount()).isZero();
        assertThat(response.skipped())
                .extracting("email", "reason")
                .containsExactly(
                        tuple("setter@gmail.com", "Đã có vai trò này"),
                        tuple("admin@gmail.com", "Là quản trị viên"),
                        tuple("bi.khoa@gmail.com", "Tài khoản đang bị khoá"),
                        tuple("da.xoa@gmail.com", "Tài khoản đã bị xoá"),
                        tuple("dang.cho@gmail.com", "Đang có lời mời chờ nhận"));
        verify(roleInvitationRepository, never()).save(any());
        verifyNoInteractions(eventPublisher, adminActionRepository);
    }

    @Test
    @DisplayName("Email trùng trong danh sách (khác hoa thường) chỉ được mời một lần")
    void invite_duplicateEmails_areInvitedOnce() {
        when(userRepository.findByEmail("an@gmail.com")).thenReturn(Optional.empty());

        InviteRolesResponse response =
                service.invite(
                        new InviteRolesRequest(
                                List.of("an@gmail.com", "AN@gmail.com "), Role.SETTER, null),
                        ADMIN);

        assertThat(response.sentCount()).isEqualTo(1);
        verify(roleInvitationRepository).save(any(RoleInvitation.class));
    }

    @Test
    @DisplayName("Chỉ mời được Người ra đề hoặc Kiểm duyệt: vai trò khác báo lỗi ở trường role")
    void invite_adminOrLearnerRole_isRejected() {
        for (Role role : List.of(Role.ADMIN, Role.LEARNER)) {
            assertThatThrownBy(
                            () ->
                                    service.invite(
                                            new InviteRolesRequest(
                                                    List.of("an@gmail.com"), role, null),
                                            ADMIN))
                    .isInstanceOfSatisfying(
                            InvalidFieldException.class,
                            ex -> assertThat(ex.getViolation().field()).isEqualTo("role"));
        }
        verify(userRepository, never()).findByEmail(anyString());
    }

    private RoleInvitation savedInvitation() {
        ArgumentCaptor<RoleInvitation> saved = ArgumentCaptor.forClass(RoleInvitation.class);
        verify(roleInvitationRepository).save(saved.capture());
        return saved.getValue();
    }

    private RoleInvitationEmailRequestedEvent publishedMail() {
        ArgumentCaptor<RoleInvitationEmailRequestedEvent> mail =
                ArgumentCaptor.forClass(RoleInvitationEmailRequestedEvent.class);
        verify(eventPublisher).publishEvent(mail.capture());
        return mail.getValue();
    }

    private AdminAction savedAction() {
        ArgumentCaptor<AdminAction> saved = ArgumentCaptor.forClass(AdminAction.class);
        verify(adminActionRepository).save(saved.capture());
        return saved.getValue();
    }

    private static User user(String username, String email, Role role) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }
}
