package com.sqljudge.common.exception;

import com.sqljudge.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public class ServiceUnavailableException extends AppException {

    public ServiceUnavailableException(ErrorCode code, String message) {
        super(code, HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
