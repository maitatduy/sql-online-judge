package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.RoleInvitationSummaryResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.entity.InvitationStatus;
import com.sqljudge.auth.entity.RoleInvitation;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.RoleInvitationRepository;
import com.sqljudge.auth.repository.RoleInvitationRow;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.security.CurrentUser;
import com.sqljudge.common.web.PageResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleInvitationAdminService {

    private static final Set<InvitationStatus> RESENDABLE =
            Set.of(InvitationStatus.PENDING, InvitationStatus.EXPIRED);

    private final UserRepository userRepository;
    private final RoleInvitationRepository roleInvitationRepository;
    private final AdminActionRepository adminActionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<RoleInvitationSummaryResponse> search(
            RoleInvitationSearch search, int page, int size) {
        Instant now = clock.instant();
        String status = search.status() == null ? null : search.status().name();
        String keyword =
                search.keyword() == null || search.keyword().isBlank()
                        ? null
                        : LikePatterns.containing(search.keyword());
        return PageResponse.from(
                roleInvitationRepository.searchInvitations(
                        status, keyword, now, PageRequest.of(page - 1, size)),
                row -> summaryOf(row, now));
    }

    @Transactional
    public void revoke(UUID invitationId, CurrentUser actor) {
        RoleInvitation invitation = lockInvitation(invitationId);
        Instant now = clock.instant();
        if (invitation.statusAt(now) != InvitationStatus.PENDING) {
            throw notPending("Chỉ huỷ được lời mời đang chờ");
        }
        invitation.revoke(now);
        log(AdminActionType.REVOKE_ROLE_INVITATION, actor, invitation);
    }

    @Transactional
    public RoleInvitationSummaryResponse resend(UUID invitationId, CurrentUser actor) {
        RoleInvitation invitation = lockInvitation(invitationId);
        Instant now = clock.instant();
        if (!RESENDABLE.contains(invitation.statusAt(now))) {
            throw notPending("Chỉ gửi lại được lời mời đang chờ hoặc đã hết hạn");
        }
        String token = SecureTokens.newToken();
        invitation.renew(RoleInvitationService.linkOf(token, now));
        log(AdminActionType.RESEND_ROLE_INVITATION, actor, invitation);
        eventPublisher.publishEvent(RoleInvitationService.mailOf(invitation, token, actor));
        return new RoleInvitationSummaryResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getRole(),
                invitation.statusAt(now),
                userRepository
                        .findById(invitation.getInvitedBy())
                        .map(User::getUsername)
                        .orElse(null),
                invitation.getCreatedAt(),
                invitation.getExpiresAt(),
                invitation.getAcceptedAt(),
                invitation.getRevokedAt());
    }

    private RoleInvitation lockInvitation(UUID invitationId) {
        return roleInvitationRepository
                .findByIdForUpdate(invitationId)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.INVITATION_NOT_FOUND, "Không tìm thấy lời mời"));
    }

    private void log(AdminActionType type, CurrentUser actor, RoleInvitation invitation) {
        User invitee = userRepository.findByEmail(invitation.getEmail()).orElse(null);
        adminActionRepository.save(AdminAction.onInvitation(type, actor.id(), invitee, invitation));
        log.info(
                "role_invitation_changed type={} invitationId={} actorId={}",
                type,
                invitation.getId(),
                actor.id());
    }

    private static RoleInvitationSummaryResponse summaryOf(RoleInvitationRow row, Instant now) {
        return new RoleInvitationSummaryResponse(
                row.id(),
                row.email(),
                row.role(),
                InvitationStatus.of(row.acceptedAt(), row.revokedAt(), row.expiresAt(), now),
                row.inviterUsername(),
                row.invitedAt(),
                row.expiresAt(),
                row.acceptedAt(),
                row.revokedAt());
    }

    private static ConflictException notPending(String message) {
        return new ConflictException(ErrorCode.INVITATION_NOT_PENDING, message);
    }
}
