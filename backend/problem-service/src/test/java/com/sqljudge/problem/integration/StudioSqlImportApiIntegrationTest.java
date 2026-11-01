package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class StudioSqlImportApiIntegrationTest extends ContainerSupport {

    private static final String PATH = "/api/studio/datasets/parse-sql";
    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String SQL =
            """
            CREATE TABLE departments (id INT PRIMARY KEY, name VARCHAR(50) NOT NULL);
            INSERT INTO departments (id, name) VALUES (1, 'Kỹ thuật'), (2, 'Kinh doanh');
            """;

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private ObjectMapper objectMapper;

    @Test
    @DisplayName("Đọc file SQL trả schema và CSV để xem trước, không ghi gì vào DB")
    void parseSql_validFile_returnsPreviewWithoutSaving() {
        ResponseEntity<String> response = post(SETTER, SQL);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext preview = JsonPath.parse(response.getBody());
        assertThat(preview.read("$.tables[0].name", String.class)).isEqualTo("departments");
        assertThat(preview.read("$.tables[0].columns[*].name", List.class))
                .containsExactly("id", "name");
        assertThat(preview.read("$.tables[0].columns[0].isPrimaryKey", Boolean.class)).isTrue();
        assertThat(preview.read("$.data[0].csv", String.class))
                .isEqualTo("id,name\n1,Kỹ thuật\n2,Kinh doanh\n");
        assertThat(preview.read("$.data[0].rowCount", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM datasets", Long.class))
                .isZero();
    }

    @Test
    @DisplayName("Câu lệnh ngoài CREATE TABLE và INSERT bị chặn với 400 ở trường sql")
    void parseSql_forbiddenStatement_returnsFieldError() {
        ResponseEntity<String> response =
                post(SETTER, "INSERT INTO a (id) VALUES (1); DROP TABLE users;");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        DocumentContext error = JsonPath.parse(response.getBody());
        assertThat(error.read("$.errors[0].field", String.class)).isEqualTo("sql");
        assertThat(error.read("$.errors[0].message", String.class))
                .isEqualTo("Câu lệnh thứ 2: chỉ nhận CREATE TABLE và INSERT");
    }

    @Test
    @DisplayName("Học viên nhận 403, chưa đăng nhập nhận 401")
    void parseSql_learnerOrAnonymous_isRejected() {
        assertThat(post(TestJwts.learnerToken(), SQL).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(post(null, SQL).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<String> post(String token, String sql) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            String body = objectMapper.writeValueAsString(Map.of("sql", sql));
            return restTemplate.exchange(
                    PATH, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
