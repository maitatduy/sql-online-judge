package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.auth.dto.request.ExportUsersRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ExportUsersRequestValidationTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(
            strings = {
                "{\"scope\":\"ALL\",\"includeEmail\":false}",
                "{\"scope\":\"FILTERED\",\"filters\":{},\"includeEmail\":false}",
                "{\"scope\":\"FILTERED\",\"filters\":{\"role\":\"SETTER\",\"status\":\"BANNED\","
                        + "\"emailVerified\":true,\"deleted\":false,\"q\":\"an\"},"
                        + "\"includeEmail\":false}",
                "{\"scope\":\"SELECTED\",\"usernames\":[\"an.bui\"],\"includeEmail\":true,"
                        + "\"reason\":\"Khảo sát\"}"
            })
    @DisplayName("Yêu cầu xuất hợp lệ không có lỗi")
    void validRequest_hasNoViolations(String json) throws Exception {
        assertThat(fields(json)).isEmpty();
    }

    @Test
    @DisplayName("Thiếu phạm vi hoặc thiếu includeEmail báo lỗi đúng trường")
    void missingScopeOrIncludeEmail_reportsField() throws Exception {
        assertThat(fields("{\"includeEmail\":false}")).containsExactly("scope");
        assertThat(fields("{\"scope\":\"ALL\"}")).containsExactly("includeEmail");
    }

    @Test
    @DisplayName("Xuất theo bộ lọc mà thiếu bộ lọc báo lỗi ở filters")
    void filteredWithoutFilters_reportsFilters() throws Exception {
        assertThat(fields("{\"scope\":\"FILTERED\",\"includeEmail\":false}"))
                .containsExactly("filters");
    }

    @Test
    @DisplayName("Xuất theo danh sách chọn cần từ 1 đến 1000 username")
    void selectedUsernames_mustHaveOneToThousand() throws Exception {
        String thousandAndOne =
                IntStream.rangeClosed(1, 1001)
                        .mapToObj(index -> "\"u" + index + "\"")
                        .collect(Collectors.joining(","));

        assertThat(fields("{\"scope\":\"SELECTED\",\"includeEmail\":false}"))
                .containsExactly("usernames");
        assertThat(fields("{\"scope\":\"SELECTED\",\"usernames\":[],\"includeEmail\":false}"))
                .containsExactly("usernames");
        assertThat(
                        fields(
                                "{\"scope\":\"SELECTED\",\"usernames\":["
                                        + thousandAndOne
                                        + "],\"includeEmail\":false}"))
                .containsExactly("usernames");
    }

    @Test
    @DisplayName("Username trong danh sách chọn dài quá 30 ký tự báo lỗi ở phần tử đó")
    void selectedUsernameTooLong_reportsElement() throws Exception {
        String json =
                "{\"scope\":\"SELECTED\",\"usernames\":[\"an.bui\",\"%s\"],\"includeEmail\":false}"
                        .formatted("a".repeat(31));

        assertThat(fields(json)).containsExactly("usernames[1].<list element>");
    }

    @Test
    @DisplayName("Xuất kèm email bắt buộc có lý do, tối đa 500 ký tự")
    void includeEmail_requiresReason() throws Exception {
        assertThat(fields("{\"scope\":\"ALL\",\"includeEmail\":true}")).containsExactly("reason");
        assertThat(fields("{\"scope\":\"ALL\",\"includeEmail\":true,\"reason\":\"  \"}"))
                .containsExactly("reason");
        assertThat(
                        fields(
                                "{\"scope\":\"ALL\",\"includeEmail\":true,\"reason\":\"%s\"}"
                                        .formatted("a".repeat(501))))
                .containsExactly("reason");
    }

    @Test
    @DisplayName("Từ khoá lọc quá 254 ký tự báo lỗi trong filters")
    void keywordTooLong_reportsFilterField() throws Exception {
        String json =
                "{\"scope\":\"FILTERED\",\"filters\":{\"q\":\"%s\"},\"includeEmail\":false}"
                        .formatted("a".repeat(255));

        assertThat(fields(json)).containsExactly("filters.q");
    }

    private static List<String> fields(String json) throws Exception {
        ExportUsersRequest request = MAPPER.readValue(json, ExportUsersRequest.class);
        return VALIDATOR.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .toList();
    }
}
