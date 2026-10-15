package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.request.RegisterRequest;
import com.sqljudge.auth.dto.response.RegisterResponse;
import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.common.security.Role;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = EmailAddresses.normalize(request.email());
        if (userRepository.existsByUsername(request.username())) {
            throw usernameTaken();
        }
        if (userRepository.existsByEmail(email)) {
            throw emailTaken();
        }
        User user = save(newLearner(request, email));
        emailVerificationService.sendVerificationEmail(user);
        return new RegisterResponse(email);
    }

    private User newLearner(RegisterRequest request, String email) {
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
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
