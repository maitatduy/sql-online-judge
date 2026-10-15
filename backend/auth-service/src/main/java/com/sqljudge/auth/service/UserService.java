package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.CurrentUserResponse;
import com.sqljudge.auth.dto.response.UsernameAvailabilityResponse;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.validation.UsernameRules;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.UnauthenticatedException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int STREAK_NOT_TRACKED_YET = 0;

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UsernameAvailabilityResponse checkUsernameAvailability(String username) {
        boolean isAvailable =
                !UsernameRules.isReserved(username) && !userRepository.existsByUsername(username);
        return new UsernameAvailabilityResponse(isAvailable);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(UUID userId) {
        User user =
                userRepository
                        .findById(userId)
                        .filter(found -> !found.isDeleted())
                        .orElseThrow(
                                () ->
                                        new UnauthenticatedException(
                                                ErrorCode.UNAUTHORIZED,
                                                "Bạn cần đăng nhập để tiếp tục"));
        String displayName =
                user.getDisplayName() == null ? user.getUsername() : user.getDisplayName();
        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                displayName,
                user.getRole(),
                STREAK_NOT_TRACKED_YET);
    }
}
