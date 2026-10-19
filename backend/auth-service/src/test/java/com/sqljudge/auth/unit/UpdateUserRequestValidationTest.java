package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.auth.dto.request.UpdateUserRequest;
import com.sqljudge.common.security.Role;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UpdateUserRequestValidationTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    @Test
    @DisplayName("Phân biệt trường không gửi với trường gửi null")
    void parse_tracksWhichFieldsWereSent() throws Exception {
        UpdateUserRequest roleOnly = parse("{\"role\":\"SETTER\",\"reason\":\"Ra đề\"}");
        UpdateUserRequest clearName = parse("{\"displayName\":null}");

        assertThat(roleOnly.isRoleProvided()).isTrue();
        assertThat(roleOnly.getRole()).isEqualTo(Role.SETTER);
        assertThat(roleOnly.isDisplayNameProvided()).isFalse();
        assertThat(clearName.isDisplayNameProvided()).isTrue();
        assertThat(clearName.getDisplayName()).isNull();
        assertThat(clearName.isRoleProvided()).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
            delimiter = '|',
            value = {
                "{\"role\":\"SETTER\",\"reason\":\"Ra đề\"}",
                "{\"displayName\":\"  Việt Lưu  \"}",
                "{\"displayName\":null}",
                "{\"role\":\"ADMIN\",\"displayName\":\"Huy Trần\"}"
            })
    @DisplayName("Yêu cầu hợp lệ không có lỗi")
    void validRequest_hasNoViolations(String json) throws Exception {
        assertThat(VALIDATOR.validate(parse(json))).isEmpty();
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(
            delimiter = '|',
            value = {
                "{}|role",
                "{\"reason\":\"Chỉ có lý do\"}|role",
                "{\"role\":null}|role",
                "{\"displayName\":\"   \"}|displayName",
                "{\"displayName\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}|displayName"
            })
    @DisplayName("Yêu cầu sai báo lỗi đúng trường")
    void invalidRequest_reportsField(String json, String field) throws Exception {
        assertThat(fields(parse(json))).containsExactly(field);
    }

    @Test
    @DisplayName("Lý do quá 500 ký tự báo lỗi ở trường reason")
    void reasonTooLong_reportsReason() throws Exception {
        String json = "{\"role\":\"SETTER\",\"reason\":\"%s\"}".formatted("a".repeat(501));

        assertThat(fields(parse(json))).containsExactly("reason");
    }

    private static UpdateUserRequest parse(String json) throws Exception {
        return MAPPER.readValue(json, UpdateUserRequest.class);
    }

    private static List<String> fields(UpdateUserRequest request) {
        return VALIDATOR.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .toList();
    }
}
