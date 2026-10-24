package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.UserEmailResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserEmailService {

    private final UserRepository userRepository;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;
    private final AdminActionRepository adminActionRepository;

    @Transactional
    public UserEmailResponse sendPasswordResetEmail(String username, UUID actorId) {
        User user = AdminUserLookup.findLive(userRepository, username);
        passwordResetService.sendResetLinkTo(user);
        adminActionRepository.save(AdminAction.sendPasswordReset(actorId, user));
        log.info("admin_password_reset_email_sent userId={} actorId={}", user.getId(), actorId);
        return new UserEmailResponse(user.getUsername());
    }

    @Transactional
    public UserEmailResponse sendVerificationEmail(String username, UUID actorId) {
        User user = AdminUserLookup.findLive(userRepository, username);
        emailVerificationService.resendVerificationEmailTo(user);
        adminActionRepository.save(AdminAction.sendVerification(actorId, user));
        log.info("admin_verification_email_sent userId={} actorId={}", user.getId(), actorId);
        return new UserEmailResponse(user.getUsername());
    }
}
