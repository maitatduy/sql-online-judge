package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TagApiIntegrationTest extends ContainerSupport {

    private static final String INSERT_TAG =
            "INSERT INTO tags (id, slug, name, created_at, updated_at)"
                    + " VALUES (UUID_TO_BIN(UUID()), ?, ?, NOW(6), NOW(6))";

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetTags() {
        jdbcTemplate.update("DELETE FROM tags");
    }

    @Test
    @DisplayName("Khách chưa đăng nhập xem được mọi tag, xếp theo tên")
    void listTags_anonymous_returnsTagsSortedByName() {
        jdbcTemplate.update(INSERT_TAG, "window-function", "Window Function");
        jdbcTemplate.update(INSERT_TAG, "cte", "CTE");
        jdbcTemplate.update(INSERT_TAG, "join", "JOIN");

        ResponseEntity<String> response = restTemplate.getForEntity("/api/tags", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody())
                .isEqualTo(
                        "{\"items\":["
                                + "{\"slug\":\"cte\",\"name\":\"CTE\"},"
                                + "{\"slug\":\"join\",\"name\":\"JOIN\"},"
                                + "{\"slug\":\"window-function\",\"name\":\"Window Function\"}"
                                + "]}");
    }

    @Test
    @DisplayName("Chưa có tag nào thì trả danh sách rỗng, không trả null")
    void listTags_noTags_returnsEmptyItems() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/tags", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("{\"items\":[]}");
    }

    @Test
    @DisplayName("Trùng slug hoặc trùng tên thì DB chặn bằng unique index")
    void insertTag_duplicateSlugOrName_isRejectedByUniqueIndex() {
        jdbcTemplate.update(INSERT_TAG, "join", "JOIN");

        assertThatThrownBy(() -> jdbcTemplate.update(INSERT_TAG, "join", "Phép nối"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(INSERT_TAG, "inner-join", "JOIN"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
