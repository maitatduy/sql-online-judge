package com.sqljudge.auth.validation;

import com.sqljudge.auth.dto.request.UpdateRestrictionsRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidRestrictionsUpdateValidator
        implements ConstraintValidator<ValidRestrictionsUpdate, UpdateRestrictionsRequest> {

    @Override
    public boolean isValid(UpdateRestrictionsRequest request, ConstraintValidatorContext context) {
        if (request == null || request.hasChange()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("aiDailyQuota")
                .addConstraintViolation();
        return false;
    }
}
