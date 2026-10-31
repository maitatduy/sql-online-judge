package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed")
class DevSeedIntegrationTest {

    private static final String SEED_DATABASE = "dev_seed";
    private static final String SEED_SCRIPT = "db/seed/R__dev_seed_problems.sql";
    private static final String ROOT_USER = "root";
    private static final int MYSQL_PORT = 3306;
    private static final String FEATURED_PROBLEM_ID = "0192f3b0-0000-7000-8000-0000000000b8";

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private DataSource dataSource;

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
    @DisplayName("Seed dev nạp 8 tag và 24 bài đã xuất bản, công khai, mỗi bài có revision")
    void seed_loadsTagsAndPublishedProblems() {
        assertThat(count("SELECT COUNT(*) FROM tags")).isEqualTo(8);
        assertThat(
                        count(
                                "SELECT COUNT(*) FROM problems WHERE status = 'PUBLISHED'"
                                        + " AND visibility = 'PUBLIC'"
                                        + " AND published_revision_id IS NOT NULL"))
                .isEqualTo(24);
        assertThat(count("SELECT COUNT(*) FROM reference_solutions")).isEqualTo(72);
        assertThat(count("SELECT COUNT(*) FROM problem_stats")).isEqualTo(24);
    }

    @Test
    @DisplayName("Thống kê seed của bài #184 khớp mock: 41,2% AC, 12.380 người giải")
    void seed_featuredProblemStatsMatchMock() {
        Long[] stats =
                jdbcTemplate.queryForObject(
                        "SELECT accepted_count, submission_count, solved_user_count"
                                + " FROM problem_stats WHERE problem_id = UUID_TO_BIN(?)",
                        (row, index) -> new Long[] {row.getLong(1), row.getLong(2), row.getLong(3)},
                        FEATURED_PROBLEM_ID);

        assertThat(stats).containsExactly(412L, 1000L, 12380L);
    }

    @Test
    @DisplayName(
            "Chuỗi trong file seed không kéo dài qua nhiều dòng, để dữ liệu không phụ thuộc kiểu"
                    + " xuống dòng khi checkout")
    void seedFile_hasNoStringLiteralSpanningLines() throws IOException {
        String script =
                new ClassPathResource(SEED_SCRIPT).getContentAsString(StandardCharsets.UTF_8);

        assertThat(literalsSpanningLines(script)).isZero();
    }

    private static int literalsSpanningLines(String script) {
        int count = 0;
        boolean insideLiteral = false;
        boolean literalHasLineBreak = false;
        for (char character : script.toCharArray()) {
            if (character == '\'') {
                if (insideLiteral && literalHasLineBreak) {
                    count += 1;
                }
                insideLiteral = !insideLiteral;
                literalHasLineBreak = false;
            } else if (insideLiteral && (character == '\n' || character == '\r')) {
                literalHasLineBreak = true;
            }
        }
        return count;
    }

    @Test
    @DisplayName(
            "Bài #184 dùng dataset Công ty đã chốt: đủ schema, 1 testcase công khai, 2 testcase ẩn"
                    + " và kết quả mong đợi")
    void seed_featuredProblemHasFullContent() {
        String revision = "(SELECT published_revision_id FROM problems WHERE id = UUID_TO_BIN(?))";

        String dataset =
                jdbcTemplate.queryForObject(
                        "SELECT d.slug FROM datasets d"
                                + " JOIN dataset_versions v ON v.dataset_id = d.id"
                                + " JOIN problem_revisions r ON r.dataset_version_id = v.id"
                                + " WHERE v.frozen_at IS NOT NULL AND r.id = "
                                + revision,
                        String.class,
                        FEATURED_PROBLEM_ID);
        List<String> tables =
                jdbcTemplate.queryForList(
                        "SELECT t.name FROM dataset_tables t"
                                + " JOIN problem_revisions r ON r.dataset_version_id = t.version_id"
                                + " WHERE r.id = "
                                + revision
                                + " ORDER BY t.position",
                        String.class,
                        FEATURED_PROBLEM_ID);
        List<Boolean> visibility =
                jdbcTemplate.queryForList(
                        "SELECT v.is_public FROM testcases t"
                                + " JOIN dataset_variants v ON v.id = t.dataset_variant_id"
                                + " WHERE t.revision_id = "
                                + revision
                                + " ORDER BY t.position",
                        Boolean.class,
                        FEATURED_PROBLEM_ID);
        List<Integer> expectedRowCounts =
                jdbcTemplate.queryForList(
                        "SELECT r.row_count FROM expected_results r"
                                + " JOIN testcases t ON t.id = r.testcase_id"
                                + " WHERE t.revision_id = "
                                + revision
                                + " ORDER BY t.position",
                        Integer.class,
                        FEATURED_PROBLEM_ID);

        assertThat(dataset).isEqualTo("cong-ty");
        assertThat(tables).containsExactly("departments", "employees");
        assertThat(visibility).containsExactly(true, false, false);
        assertThat(expectedRowCounts).containsExactly(4, 2, 3);
    }

    @Test
    @DisplayName("Chạy lại seed không nhân đôi dữ liệu")
    void seed_runTwice_keepsSameRows() {
        new ResourceDatabasePopulator(new ClassPathResource(SEED_SCRIPT)).execute(dataSource);

        assertThat(count("SELECT COUNT(*) FROM problems")).isEqualTo(24);
        assertThat(count("SELECT COUNT(*) FROM tags")).isEqualTo(8);
        assertThat(count("SELECT COUNT(*) FROM testcases")).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM datasets")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM dataset_variants")).isEqualTo(3);
    }

    private long count(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
}
