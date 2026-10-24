package com.sqljudge.common.unit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.common.exception.ForbiddenException;
import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.exception.QuotaExceededException;
import com.sqljudge.common.exception.ServiceUnavailableException;
import com.sqljudge.common.web.GlobalExceptionHandler;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-11T13:05:22Z");
    private static final ErrorCode ANY_CODE = ErrorCode.INTERNAL_ERROR;

    private MockMvc mockMvc;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        JsonMapper objectMapper =
                JsonMapper.builder()
                        .addModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .build();
        mockMvc =
                MockMvcBuilders.standaloneSetup(new ThrowingController())
                        .setControllerAdvice(
                                new GlobalExceptionHandler(Clock.fixed(NOW, ZoneOffset.UTC)))
                        .setValidator(validator)
                        .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                        .build();
    }

    @Test
    @DisplayName("Exception miền trả đúng status, code, message tiếng Việt và thời điểm")
    void appException_returnsApiErrorWithItsStatusAndCode() throws Exception {
        mockMvc.perform(get("/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy bài tập"))
                .andExpect(jsonPath("$.errors").isEmpty())
                .andExpect(jsonPath("$.timestamp").value("2026-10-11T13:05:22Z"));
    }

    @Test
    @DisplayName("Tệp tải lên vượt giới hạn multipart trả 400 VALIDATION_FAILED trên field file")
    void maxUploadSizeExceeded_returnsFileViolation() throws Exception {
        mockMvc.perform(get("/too-large"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("file"))
                .andExpect(jsonPath("$.errors[0].message").value("Tệp tải lên quá lớn"));
    }

    @Test
    @DisplayName("Lỗi một trường phát hiện trong service trả 400 VALIDATION_FAILED đúng trường")
    void invalidFieldException_returnsFieldViolation() throws Exception {
        mockMvc.perform(get("/invalid-field"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Dữ liệu gửi lên không hợp lệ"))
                .andExpect(jsonPath("$.errors[0].field").value("reason"))
                .andExpect(jsonPath("$.errors[0].message").value("Không được để trống"));
    }

    @Test
    @DisplayName("Mỗi loại exception miền ánh xạ sang đúng mã HTTP")
    void appExceptionSubclasses_mapToExpectedStatus() throws Exception {
        mockMvc.perform(get("/conflict")).andExpect(status().isConflict());
        mockMvc.perform(get("/business-rule")).andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/forbidden")).andExpect(status().isForbidden());
        mockMvc.perform(get("/unavailable")).andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("Vượt hạn mức trả 429 kèm header Retry-After tính bằng giây")
    void quotaExceeded_returns429WithRetryAfter() throws Exception {
        mockMvc.perform(get("/quota").param("retryAfterMillis", "42000"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "42"));
    }

    @Test
    @DisplayName("Retry-After làm tròn lên giây, 500 ms thành 1 giây chứ không phải 0")
    void quotaExceeded_partialSecond_roundsRetryAfterUp() throws Exception {
        mockMvc.perform(get("/quota").param("retryAfterMillis", "500"))
                .andExpect(header().string("Retry-After", "1"));
        mockMvc.perform(get("/quota").param("retryAfterMillis", "1001"))
                .andExpect(header().string("Retry-After", "2"));
    }

    @Test
    @DisplayName("Không cho tạo lỗi vượt hạn mức thiếu thời gian chờ")
    void quotaExceeded_nullRetryAfter_isRejectedAtConstruction() {
        assertThatThrownBy(() -> new QuotaExceededException(ANY_CODE, "Vượt hạn mức", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Body không hợp lệ trả 400 VALIDATION_FAILED kèm lỗi theo field")
    void invalidBody_returnsFieldViolations() throws Exception {
        mockMvc.perform(
                        post("/body")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("title"))
                .andExpect(jsonPath("$.errors[0].message").value("Không được để trống"));
    }

    @Test
    @DisplayName("Tham số không hợp lệ trả 400 VALIDATION_FAILED kèm tên tham số")
    void invalidParameter_returnsParameterViolation() throws Exception {
        mockMvc.perform(get("/param").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("page"));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
            delimiter = '|',
            value = {
                "priority|{\"title\":\"a\",\"priority\":\"URGENT\"}",
                "pinned|{\"title\":\"a\",\"pinned\":\"yes\"}"
            })
    @DisplayName("Giá trị sai kiểu trong JSON hợp lệ trả 400 VALIDATION_FAILED đúng trường")
    void unreadableValue_returnsFieldViolation(String field, String body) throws Exception {
        mockMvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value(field))
                .andExpect(jsonPath("$.errors[0].message").value("Giá trị không hợp lệ"));
    }

    @Test
    @DisplayName("JSON hỏng, sai kiểu hoặc thiếu tham số trả 400 MALFORMED_REQUEST")
    void malformedRequest_returnsMalformedRequest() throws Exception {
        mockMvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        mockMvc.perform(get("/param").param("page", "abc"))
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        mockMvc.perform(get("/param")).andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("Đường dẫn không tồn tại trả 404 RESOURCE_NOT_FOUND")
    void unknownPath_returnsNotFound() throws Exception {
        mockMvc.perform(get("/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Sai phương thức HTTP trả 405 METHOD_NOT_ALLOWED")
    void wrongMethod_returnsMethodNotAllowed() throws Exception {
        mockMvc.perform(post("/not-found"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("Lỗi xác thực trả 401 UNAUTHORIZED")
    void authenticationException_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/unauthenticated"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Người dùng đã đăng nhập nhưng thiếu quyền nhận 403 ACCESS_DENIED")
    void accessDenied_authenticatedUser_returnsForbidden() throws Exception {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(
                                "minh.nguyen", null, List.of()));

        mockMvc.perform(get("/denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Người dùng ẩn danh bị từ chối quyền nhận 401 thay vì 403")
    void accessDenied_anonymousUser_returnsUnauthorized() throws Exception {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new AnonymousAuthenticationToken(
                                "key",
                                "anonymousUser",
                                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        mockMvc.perform(get("/denied"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Chưa có thông tin xác thực bị từ chối quyền cũng nhận 401")
    void accessDenied_noAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/denied")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Lỗi không lường trước trả 500 và không lộ chi tiết kỹ thuật")
    void unexpectedException_returnsGenericInternalError() throws Exception {
        mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Đã có lỗi xảy ra, vui lòng thử lại sau"));
    }

    enum Priority {
        LOW,
        HIGH
    }

    record SampleRequest(
            @NotBlank(message = "Không được để trống") String title,
            Priority priority,
            Boolean pinned) {}

    @RestController
    static class ThrowingController {

        @GetMapping("/not-found")
        void notFound() {
            throw new NotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy bài tập");
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new ConflictException(ANY_CODE, "Xung đột");
        }

        @GetMapping("/business-rule")
        void businessRule() {
            throw new BusinessRuleException(ANY_CODE, "Sai nghiệp vụ");
        }

        @GetMapping("/invalid-field")
        void invalidField() {
            throw new InvalidFieldException("reason", "Không được để trống");
        }

        @GetMapping("/forbidden")
        void forbidden() {
            throw new ForbiddenException(ANY_CODE, "Không có quyền");
        }

        @GetMapping("/too-large")
        void tooLarge() {
            throw new MaxUploadSizeExceededException(3 * 1024 * 1024);
        }

        @GetMapping("/unavailable")
        void unavailable() {
            throw new ServiceUnavailableException(ANY_CODE, "Dịch vụ tạm ngưng");
        }

        @GetMapping("/quota")
        void quota(@RequestParam long retryAfterMillis) {
            throw new QuotaExceededException(
                    ANY_CODE, "Vượt hạn mức", Duration.ofMillis(retryAfterMillis));
        }

        @PostMapping("/body")
        void body(@Valid @RequestBody SampleRequest request) {}

        @GetMapping("/param")
        void param(@RequestParam @Min(1) int page) {}

        @GetMapping("/unauthenticated")
        void unauthenticated() {
            throw new InsufficientAuthenticationException("missing token");
        }

        @GetMapping("/denied")
        void denied() {
            throw new AccessDeniedException("denied");
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("internal detail");
        }
    }
}
