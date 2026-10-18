package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.auth.dto.request.RegisterRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RegisterRequestValidationTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    @Test
    @DisplayName("Yêu cầu hợp lệ không có lỗi")
    void validRequest_hasNoViolations() {
        assertThat(VALIDATOR.validate(request("minh.anh", "a@b.dev", "matkhau-2026", true)))
                .isEmpty();
    }

    @Test
    @DisplayName("Để trống mọi trường thì báo đúng thông báo tiếng Việt từng trường")
    void blankFields_reportVietnameseMessages() {
        RegisterRequest blank = request("", "", "", false);

        assertThat(messages(blank, "username")).contains("Vui lòng nhập tên người dùng");
        assertThat(messages(blank, "email")).contains("Vui lòng nhập email");
        assertThat(messages(blank, "password")).containsExactly("Vui lòng nhập mật khẩu");
        assertThat(messages(blank, "acceptedTerms"))
                .containsExactly("Bạn cần đồng ý với điều khoản để tiếp tục");
    }

    @ParameterizedTest
    @ValueSource(strings = {"me", "availability"})
    @DisplayName("Tên dành riêng bị từ chối")
    void reservedUsername_isRejected(String username) {
        assertThat(messages(request(username, "a@b.dev", "matkhau-2026", true), "username"))
                .contains("Tên người dùng này được hệ thống dành riêng");
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678", "password", "Password1", "QWERTY123"})
    @DisplayName("Mật khẩu phổ biến bị từ chối, không phân biệt hoa thường")
    void commonPassword_isRejected(String password) {
        assertThat(messages(request("minh.anh", "a@b.dev", password, true), "password"))
                .containsExactly("Mật khẩu này quá phổ biến, hãy chọn mật khẩu khác");
    }

    @Test
    @DisplayName("Mật khẩu dưới 8 ký tự bị từ chối")
    void shortPassword_isRejected() {
        assertThat(messages(request("minh.anh", "a@b.dev", "abc123", true), "password"))
                .containsExactly("Mật khẩu cần ít nhất 8 ký tự");
    }

    @Test
    @DisplayName("Mật khẩu vượt 72 byte UTF-8 (giới hạn của BCrypt) bị từ chối, không gây lỗi 500")
    void passwordOver72Bytes_isRejected() {
        String longVietnamese = "mậtkhẩu".repeat(9);

        assertThat(messages(request("minh.anh", "a@b.dev", longVietnamese, true), "password"))
                .containsExactly("Mật khẩu quá dài");
    }

    @Test
    @DisplayName("Email sai định dạng bị từ chối")
    void invalidEmail_isRejected() {
        assertThat(messages(request("minh.anh", "khong-phai-email", "matkhau-2026", true), "email"))
                .containsExactly("Email không hợp lệ");
    }

    private static RegisterRequest request(
            String username, String email, String password, boolean acceptedTerms) {
        return new RegisterRequest(username, email, password, acceptedTerms);
    }

    private static Set<String> messages(RegisterRequest request, String field) {
        return VALIDATOR.validate(request).stream()
                .filter(violation -> violation.getPropertyPath().toString().equals(field))
                .map(violation -> violation.getMessage())
                .collect(Collectors.toSet());
    }
}
