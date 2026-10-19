package com.sqljudge.auth.validation;

import com.sqljudge.auth.dto.request.UpdateUserRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidUserUpdateValidator
        implements ConstraintValidator<ValidUserUpdate, UpdateUserRequest> {

    static final int MAX_DISPLAY_NAME_LENGTH = 60;

    @Override
    public boolean isValid(UpdateUserRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        if (!request.isRoleProvided() && !request.isDisplayNameProvided()) {
            return reject(context, "role", "Cần vai trò hoặc tên hiển thị");
        }
        if (request.isRoleProvided() && request.getRole() == null) {
            return reject(context, "role", "Vai trò không hợp lệ");
        }
        if (request.isDisplayNameProvided() && !isValidDisplayName(request.getDisplayName())) {
            return reject(
                    context,
                    "displayName",
                    "Tên hiển thị từ 1 đến " + MAX_DISPLAY_NAME_LENGTH + " ký tự");
        }
        return true;
    }

    private static boolean isValidDisplayName(String displayName) {
        if (displayName == null) {
            return true;
        }
        String trimmed = displayName.strip();
        return !trimmed.isEmpty() && trimmed.length() <= MAX_DISPLAY_NAME_LENGTH;
    }

    private static boolean reject(
            ConstraintValidatorContext context, String field, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
        return false;
    }
}
