package com.sqljudge.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotReservedUsername.Validator.class)
public @interface NotReservedUsername {

    String message() default "Tên người dùng này được hệ thống dành riêng";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<NotReservedUsername, String> {

        @Override
        public boolean isValid(String username, ConstraintValidatorContext context) {
            return username == null || !UsernameRules.isReserved(username);
        }
    }
}
