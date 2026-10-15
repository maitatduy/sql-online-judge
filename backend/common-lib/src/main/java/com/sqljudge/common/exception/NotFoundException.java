package com.sqljudge.common.exception;

import com.sqljudge.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public class NotFoundException extends AppException {

    public NotFoundException(ErrorCode code, String message) {
        super(code, HttpStatus.NOT_FOUND, message);
    }
}
