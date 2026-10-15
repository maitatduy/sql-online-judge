package com.sqljudge.common.exception;

import com.sqljudge.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public class BusinessRuleException extends AppException {

    public BusinessRuleException(ErrorCode code, String message) {
        super(code, HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
