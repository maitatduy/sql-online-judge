package com.sqljudge.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BulkAssignableRoleValidator.class)
public @interface BulkAssignableRole {
    String message() default "Không cấp quyền Quản trị hàng loạt";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
