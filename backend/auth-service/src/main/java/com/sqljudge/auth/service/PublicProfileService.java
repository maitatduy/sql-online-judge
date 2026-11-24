package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.PublicProfileResponse;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicProfileService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PublicProfileResponse profile(String username) {
        return userRepository
                .findByUsername(username)
                .filter(user -> !user.isDeleted() && user.isEmailVerified())
                .map(PublicProfileResponse::from)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng"));
    }
}
