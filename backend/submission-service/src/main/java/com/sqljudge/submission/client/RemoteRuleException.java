package com.sqljudge.submission.client;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.AppException;
import org.springframework.http.HttpStatus;

public class RemoteRuleException extends AppException {

    public RemoteRuleException(ErrorCode code, HttpStatus status, String message) {
        super(code, status, message);
    }
}
