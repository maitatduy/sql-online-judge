package com.sqljudge.auth.validation;

import com.sqljudge.common.security.Role;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BulkAssignableRoleValidator implements ConstraintValidator<BulkAssignableRole, Role> {

    @Override
    public boolean isValid(Role role, ConstraintValidatorContext context) {
        return role != Role.ADMIN;
    }
}
