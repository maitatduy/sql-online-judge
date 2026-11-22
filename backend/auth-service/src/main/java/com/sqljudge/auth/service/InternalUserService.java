package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.InternalUserResponse;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.entity.UserRestriction;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.repository.UserRestrictionRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InternalUserService {

    private final UserRepository userRepository;
    private final UserRestrictionRepository userRestrictionRepository;

    @Transactional(readOnly = true)
    public InternalUserResponse find(UUID userId) {
        User user =
                userRepository
                        .findById(userId)
                        .filter(candidate -> !candidate.isDeleted())
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.USER_NOT_FOUND,
                                                "Không tìm thấy người dùng"));
        boolean isBannedFromRatedContests =
                userRestrictionRepository
                        .findByUserId(userId)
                        .map(UserRestriction::isBannedFromRatedContests)
                        .orElse(false);
        return new InternalUserResponse(
                user.getId(), user.getUsername(), user.getDisplayName(), isBannedFromRatedContests);
    }
}
