package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.request.InviteRolesRequest;
import com.sqljudge.auth.dto.response.InviteRolesResponse;
import com.sqljudge.auth.dto.response.SkippedInvitation;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.InvitationLink;
import com.sqljudge.auth.entity.RoleInvitation;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.event.RoleInvitationEmailRequestedEvent;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.RoleInvitationRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.common.security.CurrentUser;
import com.sqljudge.common.security.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleInvitationService {

    static final Duration INVITATION_TTL = Duration.ofDays(7);

    private static final Set<Role> INVITABLE_ROLES = Set.of(Role.SETTER, Role.MODERATOR);

    private final UserRepository userRepository;
    private final RoleInvitationRepository roleInvitationRepository;
    private final AdminActionRepository adminActionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public InviteRolesResponse invite(InviteRolesRequest request, CurrentUser actor) {
        if (!INVITABLE_ROLES.contains(request.role())) {
            throw new InvalidFieldException("role", "Chỉ mời được Người ra đề hoặc Kiểm duyệt");
        }
        Instant now = clock.instant();
        String message = messageOf(request.message());
        List<SkippedInvitation> skipped = new ArrayList<>();
        int sentCount = 0;
        for (String email : distinctEmails(request.emails())) {
            User invitee = userRepository.findByEmail(email).orElse(null);
            String skipReason = skipReasonOf(email, invitee, request.role(), now);
            if (skipReason != null) {
                skipped.add(new SkippedInvitation(email, skipReason));
                continue;
            }
            send(email, invitee, request.role(), message, actor, now);
            sentCount++;
        }
        log.info(
                "role_invitations_sent actorId={} role={} sent={} skipped={}",
                actor.id(),
                request.role(),
                sentCount,
                skipped.size());
        return new InviteRolesResponse(sentCount, skipped);
    }

    private void send(
            String email, User invitee, Role role, String message, CurrentUser actor, Instant now) {
        String token = SecureTokens.newToken();
        RoleInvitation invitation =
                RoleInvitation.of(
                        email,
                        role,
                        message,
                        actor.id(),
                        new InvitationLink(SecureTokens.sha256(token), now.plus(INVITATION_TTL)));
        roleInvitationRepository.save(invitation);
        adminActionRepository.save(AdminAction.inviteRole(actor.id(), invitee, invitation));
        eventPublisher.publishEvent(
                new RoleInvitationEmailRequestedEvent(
                        invitation.getId(), email, actor.username(), role, message, token));
    }

    private String skipReasonOf(String email, User invitee, Role role, Instant now) {
        if (invitee != null && invitee.isDeleted()) {
            return "Tài khoản đã bị xoá";
        }
        if (invitee != null && invitee.getRole() == Role.ADMIN) {
            return "Là quản trị viên";
        }
        if (invitee != null && invitee.getRole() == role) {
            return "Đã có vai trò này";
        }
        if (invitee != null && invitee.isBannedAt(now)) {
            return "Tài khoản đang bị khoá";
        }
        if (roleInvitationRepository.hasPendingInvitation(email, now)) {
            return "Đang có lời mời chờ nhận";
        }
        return null;
    }

    private static Set<String> distinctEmails(List<String> emails) {
        Set<String> distinct = new LinkedHashSet<>();
        emails.forEach(email -> distinct.add(EmailAddresses.normalize(email)));
        return distinct;
    }

    private static String messageOf(String message) {
        return message == null || message.isBlank() ? null : message.strip();
    }
}
