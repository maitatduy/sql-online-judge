package com.sqljudge.learning.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed")
class DevSeedIntegrationTest {

    private static final String SEED_DATABASE = "dev_seed";
    private static final String SEED_SCRIPT = "db/seed/R__dev_seed_courses.sql";
    private static final String ROOT_USER = "root";
    private static final int MYSQL_PORT = 3306;

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void useSeparateSeedDatabase(DynamicPropertyRegistry registry) throws SQLException {
        try (Connection connection =
                        DriverManager.getConnection(
                                ContainerSupport.MYSQL.getJdbcUrl(),
                                ROOT_USER,
                                ContainerSupport.MYSQL.getPassword());
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE IF NOT EXISTS " + SEED_DATABASE);
        }
        registry.add(
                "spring.datasource.url",
                () ->
                        "jdbc:mysql://"
                                + ContainerSupport.MYSQL.getHost()
                                + ":"
                                + ContainerSupport.MYSQL.getMappedPort(MYSQL_PORT)
                                + "/"
                                + SEED_DATABASE);
        registry.add("spring.datasource.username", () -> ROOT_USER);
        registry.add("spring.datasource.password", ContainerSupport.MYSQL::getPassword);
    }

    @Test
    @DisplayName("Seed dev có khoá SQL cơ bản đã xuất bản với 2 chương và 4 bài giảng")
    void seed_publishesBasicSqlCourse() throws Exception {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/courses/sql-co-ban", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode course = objectMapper.readTree(response.getBody());
        assertThat(course.get("lessonCount").asInt()).isEqualTo(4);
        assertThat(course.get("chapters")).hasSize(2);
        assertThat(course.get("chapters").get(0).get("lessons")).hasSize(2);
        assertThat(course.get("chapters").get(1).get("lessons")).hasSize(2);
    }

    @Test
    @DisplayName("Chuỗi trong file seed không kéo dài qua nhiều dòng")
    void seedFile_hasNoStringLiteralSpanningLines() throws IOException {
        String script =
                new ClassPathResource(SEED_SCRIPT).getContentAsString(StandardCharsets.UTF_8);

        boolean insideLiteral = false;
        int literalsSpanningLines = 0;
        boolean literalHasLineBreak = false;
        for (char character : script.toCharArray()) {
            if (character == '\'') {
                if (insideLiteral && literalHasLineBreak) {
                    literalsSpanningLines += 1;
                }
                insideLiteral = !insideLiteral;
                literalHasLineBreak = false;
            } else if (insideLiteral && (character == '\n' || character == '\r')) {
                literalHasLineBreak = true;
            }
        }

        assertThat(literalsSpanningLines).isZero();
    }
}
