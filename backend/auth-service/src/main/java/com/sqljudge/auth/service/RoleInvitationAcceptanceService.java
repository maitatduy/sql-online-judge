package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.AcceptInvitationResponse;
import com.sqljudge.auth.dto.response.RoleInvitationViewResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.InvitationStatus;
import com.sqljudge.auth.entity.RoleInvitation;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.RoleInvitationRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.common.exception.ForbiddenException;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.security.CurrentUser;
import com.sqljudge.common.security.Role;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleInvitationAcceptanceService {

    private static final int VISIBLE_LOCAL_CHARS = 2;
    private static final String MASK = "***";

    private final UserRepository userRepository;
    private final RoleInvitationRepository roleInvitationRepository;
    private final AdminActionRepository adminActionRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public RoleInvitationViewResponse view(String token) {
        RoleInvitation invitation =
                roleInvitationRepository
                        .findByTokenHash(SecureTokens.sha256(token))
                        .orElseThrow(RoleInvitationAcceptanceService::notFound);
        String inviterUsername =
                userRepository
                        .findById(invitation.getInvitedBy())
                        .map(User::getUsername)
                        .orElse(null);
        return new RoleInvitationViewResponse(
                mask(invitation.getEmail()),
                invitation.getRole(),
                inviterUsername,
                invitation.getExpiresAt(),
                invitation.statusAt(clock.instant()));
    }

    @Transactional
    public AcceptInvitationResponse accept(String token, CurrentUser actor) {
        RoleInvitation invitation =
                roleInvitationRepository
                        .findByTokenHashForUpdate(SecureTokens.sha256(token))
                        .orElseThrow(RoleInvitationAcceptanceService::notFound);
        User invitee =
                userRepository
                        .findById(actor.id())
                        .orElseThrow(RoleInvitationAcceptanceService::notFound);
        Instant now = clock.instant();
        requireAcceptable(invitation, invitee, now);
        Role previousRole = invitee.getRole();
        invitee.setRole(invitation.getRole());
        invitation.accept(invitee.getId(), now);
        adminActionRepository.save(AdminAction.acceptRoleInvitation(invitee, previousRole));
        log.info(
                "role_invitation_accepted invitationId={} userId={} role={}",
                invitation.getId(),
                invitee.getId(),
                invitation.getRole());
        return new AcceptInvitationResponse(invitee.getRole());
    }

    private static void requireAcceptable(RoleInvitation invitation, User invitee, Instant now) {
        InvitationStatus status = invitation.statusAt(now);
        if (status == InvitationStatus.ACCEPTED) {
            throw new ConflictException(
                    ErrorCode.INVITATION_ALREADY_ACCEPTED, "Lời mời này đã được nhận");
        }
        if (status == InvitationStatus.REVOKED) {
            throw new BusinessRuleException(ErrorCode.INVITATION_REVOKED, "Lời mời này đã bị huỷ");
        }
        if (status == InvitationStatus.EXPIRED) {
            throw new BusinessRuleException(ErrorCode.INVITATION_EXPIRED, "Lời mời này đã hết hạn");
        }
        if (!EmailAddresses.normalize(invitee.getEmail()).equals(invitation.getEmail())) {
            throw new ForbiddenException(
                    ErrorCode.INVITATION_EMAIL_MISMATCH,
                    "Lời mời này gửi tới email khác với tài khoản đang đăng nhập");
        }
        if (invitee.getRole() == Role.ADMIN) {
            throw new BusinessRuleException(
                    ErrorCode.INVITATION_NOT_APPLICABLE,
                    "Tài khoản quản trị không cần nhận lời mời này");
        }
    }

    private static String mask(String email) {
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        int visible = Math.min(VISIBLE_LOCAL_CHARS, Math.max(1, local.length() - 1));
        return local.substring(0, visible) + MASK + email.substring(at);
    }

    private static NotFoundException notFound() {
        return new NotFoundException(ErrorCode.INVITATION_NOT_FOUND, "Không tìm thấy lời mời");
    }
}
