# Xử lý lỗi

## Nguyên tắc

- Lỗi nghiệp vụ ném exception có tên rõ nghĩa; xử lý tập trung một chỗ (backend: `@RestControllerAdvice`, frontend: error boundary + handler của TanStack Query).
- Không nuốt lỗi (`catch` rỗng, `catch` chỉ log rồi trả `null`).
- Không dùng try-catch bọc từng lời gọi repository. Chỉ catch khi thật sự xử lý được: gọi Feign cần fallback, retry có giới hạn, chuyển lỗi DB sandbox thành verdict `RE`.
- Người dùng thấy thông báo tiếng Việt dễ hiểu; log ghi chi tiết kỹ thuật kèm `traceId`.

---

## Backend

### Hệ exception trong `common-lib`

```java
public abstract class AppException extends RuntimeException {
    private final ErrorCode code;
    private final HttpStatus status;
    // constructor nhận code, status, message tiếng Việt
}

public class NotFoundException extends AppException { … }        // 404
public class ConflictException extends AppException { … }        // 409
public class BusinessRuleException extends AppException { … }   // 422
public class ForbiddenException extends AppException { … }       // 403
public class QuotaExceededException extends AppException { … }   // 429
```

`ErrorCode` là enum dùng chung, ví dụ: `PROBLEM_NOT_FOUND`, `CONTEST_NOT_STARTED`, `CONTEST_ALREADY_FINISHED`, `SUBMISSION_RATE_LIMITED`, `AI_QUOTA_EXCEEDED`, `AI_DISABLED_DURING_CONTEST`, `USERNAME_TAKEN`.

### Ném lỗi

```java
public ProblemDetailResponse getBySlug(String slug) {
    Problem problem = problemRepository.findBySlugAndStatus(slug, PUBLISHED)
        .orElseThrow(() -> new NotFoundException(ErrorCode.PROBLEM_NOT_FOUND, "Không tìm thấy bài tập"));
    return problemMapper.toDetail(problem);
}
```

### Handler tập trung

```java
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    ResponseEntity<ApiError> handleApp(AppException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiError.of(ex));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(ApiError.validation(ex.getBindingResult()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("unexpected_error", ex);                     // stack trace chỉ vào log
        return ResponseEntity.internalServerError()
            .body(ApiError.internal("Đã có lỗi xảy ra, vui lòng thử lại sau"));
    }
}
```

Định dạng `ApiError` xem `api-conventions.md`.

### Gọi service khác

- Mọi Feign client có timeout (connect 1 s, read 3 s), circuit breaker và fallback của Resilience4j.
- Fallback trả dữ liệu thay thế hợp lý (ví dụ: không lấy được avatar thì trả avatar mặc định) hoặc ném `ServiceUnavailableException` (503) - không trả dữ liệu sai.

### Kafka consumer

- Lỗi tạm thời: retry có backoff (3 lần).
- Lỗi vĩnh viễn hoặc hết retry: đẩy sang topic `.dlt` (dead letter), log kèm key, không làm kẹt partition.
- Consumer phải idempotent (xem `system-design.md`).

### Lỗi trong máy chấm - không phải lỗi API

| Tình huống | Kết quả |
|---|---|
| Lỗi cú pháp / lỗi thực thi do SQL người dùng | Verdict `RE` + thông báo đã lọc |
| Hết thời gian | Verdict `TLE` |
| Sandbox không khởi tạo được, mất kết nối | Verdict `SE`, tự đưa lại hàng đợi (tối đa 2 lần), cảnh báo admin |

---

## Frontend

- Mỗi route group có `error.tsx` (lỗi bất ngờ) và `not-found.tsx`.
- `api-client` đọc `ApiError` và ném `ApiRequestError` có `status`, `code`, `message`, `errors`.
- Xử lý theo `code`, không theo chuỗi `message`:
  - `401` → thử refresh token một lần, thất bại thì về trang đăng nhập kèm `?next=`.
  - `403 AI_DISABLED_DURING_CONTEST` → hiện trạng thái khoá trên nút AI, không toast lỗi.
  - `429` → hiện thời gian chờ từ `Retry-After`.
  - `400` có `errors` → gắn lỗi vào đúng field của form (react-hook-form `setError`).
- Lỗi mutation hiện toast; lỗi tải trang hiện trạng thái lỗi trong vùng nội dung kèm nút "Thử lại" (theo bảng trạng thái mục 7 của thiết kế).
- Mất kết nối SSE khi đang chờ verdict: tự kết nối lại, fallback sang polling 2 giây, hiện toast "Mất kết nối máy chấm, đang thử lại…". Code trong editor vẫn được lưu nháp.
