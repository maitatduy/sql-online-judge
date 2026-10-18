package com.sqljudge.common.exception;

import com.sqljudge.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public class UnauthenticatedException extends AppException {

    public UnauthenticatedException(ErrorCode code, String message) {
        super(code, HttpStatus.UNAUTHORIZED, message);
    }
}
