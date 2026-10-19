package com.sqljudge.common.web;

import com.sqljudge.common.error.ApiError;
import com.sqljudge.common.error.ErrorCode;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class OptimisticLockExceptionHandler {

    private final Clock clock;

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiError> handleConcurrentModification(OptimisticLockingFailureException ex) {
        log.warn("concurrent_modification reason={}", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                        ApiError.of(
                                HttpStatus.CONFLICT,
                                ErrorCode.CONCURRENT_MODIFICATION,
                                "Dữ liệu vừa được người khác sửa, vui lòng tải lại",
                                clock.instant()));
    }
}
