package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.problem.entity.DatasetVersion;
import com.sqljudge.problem.entity.ExpectedResult;
import com.sqljudge.problem.repository.DatasetVersionRepository;
import com.sqljudge.problem.service.DatasetChecksum;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(CommonAutoConfiguration.class)
@TestPropertySource(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed")
class SeedDatasetChecksumTest {

    private static final String SEED_DATABASE = "dev_seed_checksum";
    private static final String ROOT_USER = "root";
    private static final int MYSQL_PORT = 3306;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private DatasetVersionRepository datasetVersionRepository;
    @Autowired private TestEntityManager entityManager;

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
    @DisplayName("Mọi phiên bản dataset của seed đã chốt và có checksum khớp với nội dung thật")
    void seedDatasetVersions_areFrozenWithMatchingChecksum() {
        List<DatasetVersion> versions = datasetVersionRepository.findAll();

        assertThat(versions).isNotEmpty();
        assertThat(versions)
                .allSatisfy(
                        version -> {
                            assertThat(version.getFrozenAt()).isNotNull();
                            assertThat(version.getChecksum())
                                    .as("checksum of version %s", version.getId())
                                    .isEqualTo(DatasetChecksum.of(version));
                        });
    }

    @Test
    @DisplayName("Kết quả mong đợi của seed lưu đúng dạng ô {kind, value} mà máy chấm dùng")
    void seedExpectedResults_useWireCellsWithMatchingChecksum() throws Exception {
        List<ExpectedResult> results =
                entityManager
                        .getEntityManager()
                        .createQuery("SELECT e FROM ExpectedResult e", ExpectedResult.class)
                        .getResultList();

        assertThat(results).isNotEmpty();
        for (ExpectedResult result : results) {
            JsonNode rows = objectMapper.readTree(result.getRowsJson());
            assertThat(rows).hasSize(result.getRowCount());
            assertThat(rows.valueStream().flatMap(JsonNode::valueStream).toList())
                    .allSatisfy(
                            cell -> {
                                assertThat(cell.hasNonNull("kind")).as("cell %s", cell).isTrue();
                                assertThat(cell.has("value")).as("cell %s", cell).isTrue();
                            });
            assertThat(result.getChecksum())
                    .as("checksum of expected result %s", result.getId())
                    .isEqualTo(DatasetChecksum.sha256(result.getRowsJson()));
        }
    }
}
