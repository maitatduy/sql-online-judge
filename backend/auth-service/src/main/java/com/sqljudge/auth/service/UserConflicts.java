package com.sqljudge.auth.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ConflictException;
import org.springframework.dao.DataIntegrityViolationException;

final class UserConflicts {

    private static final String EMAIL_UNIQUE_INDEX = "uk_users_email";

    private UserConflicts() {}

    static ConflictException usernameTaken() {
        return new ConflictException(
                ErrorCode.USERNAME_TAKEN, "Tên người dùng này đã có người dùng");
    }

    static ConflictException emailTaken() {
        return new ConflictException(ErrorCode.EMAIL_TAKEN, "Email này đã được đăng ký");
    }

    static ConflictException fromUniqueViolation(DataIntegrityViolationException exception) {
        String detail = String.valueOf(exception.getMostSpecificCause().getMessage());
        return detail.contains(EMAIL_UNIQUE_INDEX) ? emailTaken() : usernameTaken();
    }
}
