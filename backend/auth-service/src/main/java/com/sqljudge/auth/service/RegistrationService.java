package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.request.RegisterRequest;
import com.sqljudge.auth.dto.response.RegisterResponse;
import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.event.AccountExistsEmailRequestedEvent;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.common.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final String EMAIL_UNIQUE_INDEX = "uk_users_email";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = EmailAddresses.normalize(request.email());
        if (userRepository.existsByUsername(request.username())) {
            throw usernameTaken();
        }
        String passwordHash = passwordEncoder.encode(request.password());
        userRepository
                .findByEmail(email)
                .ifPresentOrElse(
                        this::notifyExistingOwner,
                        () -> registerNewLearner(request, email, passwordHash));
        return new RegisterResponse(email);
    }

    private void registerNewLearner(RegisterRequest request, String email, String passwordHash) {
        User user = save(newLearner(request, email, passwordHash));
        emailVerificationService.sendVerificationEmail(user);
    }

    private void notifyExistingOwner(User owner) {
        if (owner.isDeleted()) {
            return;
        }
        eventPublisher.publishEvent(
                new AccountExistsEmailRequestedEvent(
                        owner.getId(), owner.getEmail(), owner.getUsername()));
    }

    private static User newLearner(RegisterRequest request, String email, String passwordHash) {
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setRole(Role.LEARNER);
        user.setSignupProvider(SignupProvider.EMAIL);
        return user;
    }

    private User save(User user) {
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            String detail = String.valueOf(exception.getMostSpecificCause().getMessage());
            throw detail.contains(EMAIL_UNIQUE_INDEX) ? emailTaken() : usernameTaken();
        }
    }

    private static ConflictException usernameTaken() {
        return new ConflictException(
                ErrorCode.USERNAME_TAKEN, "Tên người dùng này đã có người dùng");
    }

    private static ConflictException emailTaken() {
        return new ConflictException(ErrorCode.EMAIL_TAKEN, "Email này đã được đăng ký");
    }
}
