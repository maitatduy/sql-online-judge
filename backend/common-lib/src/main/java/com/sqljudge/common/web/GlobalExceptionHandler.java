package com.sqljudge.common.web;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.sqljudge.common.error.ApiError;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.error.FieldViolation;
import com.sqljudge.common.exception.AppException;
import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.common.exception.QuotaExceededException;
import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final String VALIDATION_MESSAGE = "Dữ liệu gửi lên không hợp lệ";
    private static final String INVALID_VALUE_MESSAGE = "Giá trị không hợp lệ";

    private static final AuthenticationTrustResolver TRUST_RESOLVER =
            new AuthenticationTrustResolverImpl();

    private final Clock clock;

    @ExceptionHandler(QuotaExceededException.class)
    ResponseEntity<ApiError> handleQuotaExceeded(QuotaExceededException ex) {
        return ResponseEntity.status(ex.getStatus())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.retryAfterSeconds()))
                .body(ApiError.of(ex.getStatus(), ex.getCode(), ex.getMessage(), clock.instant()));
    }

    @ExceptionHandler(InvalidFieldException.class)
    ResponseEntity<ApiError> handleInvalidField(InvalidFieldException ex) {
        return respondWithViolations(List.of(ex.getViolation()));
    }

    @ExceptionHandler(AppException.class)
    ResponseEntity<ApiError> handleApp(AppException ex) {
        return respond(ex.getStatus(), ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleInvalidBody(MethodArgumentNotValidException ex) {
        List<FieldViolation> violations =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(
                                error ->
                                        new FieldViolation(
                                                error.getField(), error.getDefaultMessage()))
                        .toList();
        return respondWithViolations(violations);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> handleInvalidParameters(HandlerMethodValidationException ex) {
        List<FieldViolation> violations =
                ex.getParameterValidationResults().stream()
                        .flatMap(GlobalExceptionHandler::toViolations)
                        .toList();
        return respondWithViolations(violations);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof MismatchedInputException mismatch
                && !mismatch.getPath().isEmpty()) {
            return respondWithViolations(
                    List.of(new FieldViolation(fieldPath(mismatch), INVALID_VALUE_MESSAGE)));
        }
        return respondMalformed();
    }

    @ExceptionHandler({
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class
    })
    ResponseEntity<ApiError> handleMalformedRequest(Exception ex) {
        return respondMalformed();
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<ApiError> handleNoResource(Exception ex) {
        return respond(
                HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy tài nguyên");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return respond(
                HttpStatus.METHOD_NOT_ALLOWED,
                ErrorCode.METHOD_NOT_ALLOWED,
                "Phương thức không được hỗ trợ");
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        return respond(
                HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, SecurityMessages.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || TRUST_RESOLVER.isAnonymous(authentication)) {
            return respond(
                    HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, SecurityMessages.UNAUTHORIZED);
        }
        return respond(
                HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, SecurityMessages.ACCESS_DENIED);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("unexpected_error", ex);
        return respond(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                "Đã có lỗi xảy ra, vui lòng thử lại sau");
    }

    private ResponseEntity<ApiError> respond(HttpStatus status, ErrorCode code, String message) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status, code, message, clock.instant()));
    }

    private ResponseEntity<ApiError> respondMalformed() {
        return respond(
                HttpStatus.BAD_REQUEST,
                ErrorCode.MALFORMED_REQUEST,
                "Yêu cầu không đúng định dạng");
    }

    private ResponseEntity<ApiError> respondWithViolations(List<FieldViolation> violations) {
        return ResponseEntity.badRequest()
                .body(
                        ApiError.of(
                                HttpStatus.BAD_REQUEST,
                                ErrorCode.VALIDATION_FAILED,
                                VALIDATION_MESSAGE,
                                violations,
                                clock.instant()));
    }

    private static String fieldPath(MismatchedInputException mismatch) {
        return mismatch.getPath().stream()
                .map(
                        reference ->
                                reference.getFieldName() != null
                                        ? reference.getFieldName()
                                        : String.valueOf(reference.getIndex()))
                .collect(Collectors.joining("."));
    }

    private static Stream<FieldViolation> toViolations(ParameterValidationResult result) {
        String parameterName = result.getMethodParameter().getParameterName();
        return result.getResolvableErrors().stream()
                .map(error -> toViolation(parameterName, error));
    }

    private static FieldViolation toViolation(String parameterName, MessageSourceResolvable error) {
        String field =
                error instanceof FieldError fieldError ? fieldError.getField() : parameterName;
        return new FieldViolation(field, error.getDefaultMessage());
    }
}
