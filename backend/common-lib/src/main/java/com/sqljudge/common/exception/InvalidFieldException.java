package com.sqljudge.common.exception;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.error.FieldViolation;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InvalidFieldException extends AppException {

    private final FieldViolation violation;

    public InvalidFieldException(String field, String message) {
        super(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ");
        this.violation = new FieldViolation(field, message);
    }
}
