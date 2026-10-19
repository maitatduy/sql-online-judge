package com.sqljudge.auth.service;

import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;

final class AdminUserLookup {

    private AdminUserLookup() {}

    static User findAny(UserRepository userRepository, String username) {
        return userRepository.findByUsername(username).orElseThrow(AdminUserLookup::notFound);
    }

    static User findLive(UserRepository userRepository, String username) {
        return userRepository
                .findByUsername(username)
                .filter(user -> !user.isDeleted())
                .orElseThrow(AdminUserLookup::notFound);
    }

    private static NotFoundException notFound() {
        return new NotFoundException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng");
    }
}
