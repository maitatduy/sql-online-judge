package com.sqljudge.auth.validation;

import com.sqljudge.auth.dto.request.ExportScope;
import com.sqljudge.auth.dto.request.ExportUsersRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;

public class ValidUserExportValidator
        implements ConstraintValidator<ValidUserExport, ExportUsersRequest> {

    static final int MAX_SELECTED_USERNAMES = 1000;

    @Override
    public boolean isValid(ExportUsersRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        if (request.scope() == ExportScope.SELECTED && !isUsernameList(request.usernames())) {
            return reject(
                    context, "usernames", "Chọn từ 1 đến " + MAX_SELECTED_USERNAMES + " tài khoản");
        }
        if (request.scope() == ExportScope.FILTERED && request.filters() == null) {
            return reject(context, "filters", "Thiếu bộ lọc");
        }
        if (request.isEmailIncluded() && isBlank(request.reason())) {
            return reject(context, "reason", "Không được để trống");
        }
        return true;
    }

    private static boolean isUsernameList(List<String> usernames) {
        return usernames != null
                && !usernames.isEmpty()
                && usernames.size() <= MAX_SELECTED_USERNAMES
                && usernames.stream().noneMatch(ValidUserExportValidator::isBlank);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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
