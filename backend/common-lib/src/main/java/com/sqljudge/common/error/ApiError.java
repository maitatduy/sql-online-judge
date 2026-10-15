package com.sqljudge.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        int status,
        ErrorCode code,
        String message,
        List<FieldViolation> errors,
        String traceId,
        Instant timestamp) {

    private static final String TRACE_ID_MDC_KEY = "traceId";

    public static ApiError of(
            HttpStatus status, ErrorCode code, String message, Instant timestamp) {
        return of(status, code, message, List.of(), timestamp);
    }

    public static ApiError of(
            HttpStatus status,
            ErrorCode code,
            String message,
            List<FieldViolation> errors,
            Instant timestamp) {
        return new ApiError(
                status.value(),
                code,
                message,
                List.copyOf(errors),
                MDC.get(TRACE_ID_MDC_KEY),
                timestamp);
    }
}
