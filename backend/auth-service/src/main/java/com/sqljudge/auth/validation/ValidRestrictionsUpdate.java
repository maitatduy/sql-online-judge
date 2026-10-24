package com.sqljudge.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidRestrictionsUpdateValidator.class)
public @interface ValidRestrictionsUpdate {
    String message() default "Cần ít nhất một thay đổi";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
