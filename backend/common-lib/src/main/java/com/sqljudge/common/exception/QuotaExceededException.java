package com.sqljudge.common.exception;

import com.sqljudge.common.error.ErrorCode;
import java.time.Duration;
import java.util.Objects;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class QuotaExceededException extends AppException {

    private static final long MILLIS_PER_SECOND = 1000;

    private final Duration retryAfter;

    public QuotaExceededException(ErrorCode code, String message, Duration retryAfter) {
        super(code, HttpStatus.TOO_MANY_REQUESTS, message);
        this.retryAfter = Objects.requireNonNull(retryAfter, "retryAfter must not be null");
    }

    public long retryAfterSeconds() {
        return (retryAfter.toMillis() + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND;
    }
}
