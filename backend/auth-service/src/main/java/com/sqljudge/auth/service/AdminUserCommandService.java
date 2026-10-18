package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.request.CreateUserRequest;
import com.sqljudge.auth.dto.response.AdminUserDetailResponse;
import com.sqljudge.auth.entity.EmailToken;
import com.sqljudge.auth.entity.EmailTokenPurpose;
import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.event.InvitationEmailRequestedEvent;
import com.sqljudge.auth.repository.EmailTokenRepository;
import com.sqljudge.auth.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserCommandService {

    static final Duration INVITE_LINK_TTL = Duration.ofDays(7);

    private final UserRepository userRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public AdminUserDetailResponse createUser(CreateUserRequest request) {
        String email = EmailAddresses.normalize(request.email());
        if (userRepository.existsByUsername(request.username())) {
            throw UserConflicts.usernameTaken();
        }
        if (userRepository.existsByEmail(email)) {
            throw UserConflicts.emailTaken();
        }
        Instant now = clock.instant();
        User user = save(newInvitedUser(request, email));
        sendInvitation(user, now);
        log.info(
                "admin_user_created userId={} createdBy={} role={}",
                user.getId(),
                user.getCreatedBy(),
                user.getRole());
        return AdminUserViews.toDetail(user, now);
    }

    private void sendInvitation(User user, Instant now) {
        String token = SecureTokens.newToken();
        EmailToken invite = new EmailToken();
        invite.setUser(user);
        invite.setPurpose(EmailTokenPurpose.INVITE);
        invite.setTokenHash(SecureTokens.sha256(token));
        invite.setExpiresAt(now.plus(INVITE_LINK_TTL));
        emailTokenRepository.save(invite);
        eventPublisher.publishEvent(
                new InvitationEmailRequestedEvent(
                        user.getId(), user.getEmail(), user.getUsername(), token));
    }

    private User save(User user) {
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw UserConflicts.fromUniqueViolation(exception);
        }
    }

    private static User newInvitedUser(CreateUserRequest request, String email) {
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(email);
        user.setRole(request.role());
        user.setDisplayName(trimToNull(request.displayName()));
        user.setSignupProvider(SignupProvider.EMAIL);
        return user;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
