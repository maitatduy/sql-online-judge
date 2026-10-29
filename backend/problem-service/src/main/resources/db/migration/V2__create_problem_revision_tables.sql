CREATE TABLE problems (
    id BINARY(16) NOT NULL PRIMARY KEY,
    slug VARCHAR(160) NOT NULL,
    number INT NULL,
    title VARCHAR(200) NOT NULL,
    difficulty VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    visibility VARCHAR(20) NULL,
    author_id BINARY(16) NOT NULL,
    statement_markdown MEDIUMTEXT NOT NULL,
    starter_code TEXT NOT NULL,
    time_limit_ms INT NOT NULL,
    requires_order BOOLEAN NOT NULL,
    compare_column_names BOOLEAN NOT NULL,
    float_tolerance DOUBLE NOT NULL,
    allowed_statements VARCHAR(64) NOT NULL,
    draft_revision_id BINARY(16) NULL,
    published_revision_id BINARY(16) NULL,
    published_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    version BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_problems_slug UNIQUE (slug),
    CONSTRAINT uk_problems_number UNIQUE (number)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_problems_status_visibility_number ON problems (status, visibility, number);

CREATE INDEX idx_problems_author_id_updated_at ON problems (author_id, updated_at);

CREATE TABLE problem_tags (
    problem_id BINARY(16) NOT NULL,
    tag_id BINARY(16) NOT NULL,
    PRIMARY KEY (problem_id, tag_id),
    CONSTRAINT fk_problem_tags_problems FOREIGN KEY (problem_id) REFERENCES problems (id) ON DELETE CASCADE,
    CONSTRAINT fk_problem_tags_tags FOREIGN KEY (tag_id) REFERENCES tags (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_problem_tags_tag_id ON problem_tags (tag_id);

CREATE TABLE problem_revisions (
    id BINARY(16) NOT NULL PRIMARY KEY,
    problem_id BINARY(16) NOT NULL,
    number INT NOT NULL,
    frozen_at DATETIME(6) NULL,
    checksum CHAR(64) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_problem_revisions_problem_id_number UNIQUE (problem_id, number),
    CONSTRAINT fk_problem_revisions_problems FOREIGN KEY (problem_id) REFERENCES problems (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

ALTER TABLE problems
    ADD CONSTRAINT fk_problems_draft_revision FOREIGN KEY (draft_revision_id) REFERENCES problem_revisions (id),
    ADD CONSTRAINT fk_problems_published_revision FOREIGN KEY (published_revision_id) REFERENCES problem_revisions (id);

CREATE TABLE revision_tables (
    id BINARY(16) NOT NULL PRIMARY KEY,
    revision_id BINARY(16) NOT NULL,
    name VARCHAR(64) NOT NULL,
    position INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_revision_tables_revision_id_name UNIQUE (revision_id, name),
    CONSTRAINT fk_revision_tables_problem_revisions FOREIGN KEY (revision_id) REFERENCES problem_revisions (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE revision_columns (
    id BINARY(16) NOT NULL PRIMARY KEY,
    table_id BINARY(16) NOT NULL,
    name VARCHAR(64) NOT NULL,
    logical_type VARCHAR(16) NOT NULL,
    is_primary_key BOOLEAN NOT NULL,
    is_nullable BOOLEAN NOT NULL,
    references_table VARCHAR(64) NULL,
    references_column VARCHAR(64) NULL,
    position INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_revision_columns_table_id_name UNIQUE (table_id, name),
    CONSTRAINT fk_revision_columns_revision_tables FOREIGN KEY (table_id) REFERENCES revision_tables (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE reference_solutions (
    id BINARY(16) NOT NULL PRIMARY KEY,
    revision_id BINARY(16) NOT NULL,
    dialect VARCHAR(16) NOT NULL,
    code TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_reference_solutions_revision_id_dialect UNIQUE (revision_id, dialect),
    CONSTRAINT fk_reference_solutions_problem_revisions FOREIGN KEY (revision_id) REFERENCES problem_revisions (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE testcases (
    id BINARY(16) NOT NULL PRIMARY KEY,
    revision_id BINARY(16) NOT NULL,
    position INT NOT NULL,
    description VARCHAR(200) NOT NULL,
    is_public BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT fk_testcases_problem_revisions FOREIGN KEY (revision_id) REFERENCES problem_revisions (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_testcases_revision_id_position ON testcases (revision_id, position);

CREATE TABLE testcase_data (
    id BINARY(16) NOT NULL PRIMARY KEY,
    testcase_id BINARY(16) NOT NULL,
    table_name VARCHAR(64) NOT NULL,
    csv LONGTEXT NOT NULL,
    row_count INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_testcase_data_testcase_id_table_name UNIQUE (testcase_id, table_name),
    CONSTRAINT fk_testcase_data_testcases FOREIGN KEY (testcase_id) REFERENCES testcases (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE expected_results (
    id BINARY(16) NOT NULL PRIMARY KEY,
    testcase_id BINARY(16) NOT NULL,
    columns_json JSON NOT NULL,
    rows_json LONGTEXT NOT NULL,
    row_count INT NOT NULL,
    checksum CHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_expected_results_testcase_id UNIQUE (testcase_id),
    CONSTRAINT fk_expected_results_testcases FOREIGN KEY (testcase_id) REFERENCES testcases (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
