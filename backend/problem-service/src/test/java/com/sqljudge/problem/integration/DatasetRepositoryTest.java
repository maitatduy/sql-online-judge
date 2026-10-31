package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.problem.entity.Dataset;
import com.sqljudge.problem.entity.DatasetColumn;
import com.sqljudge.problem.entity.DatasetTable;
import com.sqljudge.problem.entity.DatasetVariant;
import com.sqljudge.problem.entity.DatasetVariantTable;
import com.sqljudge.problem.entity.DatasetVersion;
import com.sqljudge.problem.entity.LogicalType;
import com.sqljudge.problem.repository.DatasetRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(CommonAutoConfiguration.class)
class DatasetRepositoryTest extends ContainerSupport {

    @Autowired private DatasetRepository datasetRepository;

    @Autowired private EntityManager entityManager;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Lưu dataset cùng phiên bản, schema và bộ dữ liệu rồi đọc lại đủ")
    void save_datasetWithVersion_roundTripsWholeGraph() {
        datasetRepository.saveAndFlush(ProblemFixtures.companyDataset("cong-ty"));
        entityManager.clear();

        Dataset loaded = datasetRepository.findBySlug("cong-ty").orElseThrow();

        assertThat(loaded.getId().version()).isEqualTo(7);
        DatasetVersion version = loaded.getVersions().get(0);
        assertThat(version.getNumber()).isEqualTo(1);
        assertThat(version.getFrozenAt()).isNotNull();
        assertThat(version.getTables())
                .extracting(DatasetTable::getName)
                .containsExactly("departments", "employees");
        assertThat(version.getTables().get(1).getColumns())
                .extracting(
                        DatasetColumn::getName,
                        DatasetColumn::getLogicalType,
                        DatasetColumn::getReferencesTable)
                .containsExactly(
                        tuple("id", LogicalType.INTEGER, null),
                        tuple("department_id", LogicalType.INTEGER, "departments"));
        assertThat(version.getVariants())
                .extracting(DatasetVariant::getName, DatasetVariant::isPublic)
                .containsExactly(tuple("Mẫu", true), tuple("Nhiều người cùng lương", false));
        assertThat(version.getVariants().get(0).getTables())
                .extracting(DatasetVariantTable::getTableName, DatasetVariantTable::getRowCount)
                .containsExactly(tuple("departments", 2), tuple("employees", 3));
    }

    @Test
    @DisplayName("Trùng slug dataset thì DB chặn bằng unique index")
    void save_duplicateSlug_isRejected() {
        datasetRepository.saveAndFlush(ProblemFixtures.companyDataset("trung-dataset"));

        assertThatThrownBy(
                        () ->
                                datasetRepository.saveAndFlush(
                                        ProblemFixtures.companyDataset("trung-dataset")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Một dataset không có hai phiên bản cùng số")
    void save_twoVersionsWithSameNumber_isRejected() {
        Dataset dataset = ProblemFixtures.companyDataset("hai-phien-ban");
        dataset.addVersion(1);

        assertThatThrownBy(() -> datasetRepository.saveAndFlush(dataset))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Xoá phiên bản thì xoá luôn bảng, cột, bộ dữ liệu và CSV của nó")
    void removeVersion_deletesItsSchemaAndVariants() {
        Dataset dataset =
                datasetRepository.saveAndFlush(ProblemFixtures.companyDataset("xoa-phien-ban"));

        dataset.getVersions().remove(0);
        datasetRepository.saveAndFlush(dataset);

        assertThat(count("dataset_versions")).isZero();
        assertThat(count("dataset_tables")).isZero();
        assertThat(count("dataset_columns")).isZero();
        assertThat(count("dataset_variants")).isZero();
        assertThat(count("dataset_variant_tables")).isZero();
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }
}
