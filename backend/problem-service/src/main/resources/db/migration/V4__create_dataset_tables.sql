CREATE TABLE datasets (
    id BINARY(16) NOT NULL PRIMARY KEY,
    owner_id BINARY(16) NOT NULL,
    slug VARCHAR(160) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NOT NULL,
    deleted_at DATETIME(6) NULL,
    version BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_datasets_slug UNIQUE (slug)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_datasets_owner_id_updated_at ON datasets (owner_id, updated_at);

CREATE TABLE dataset_versions (
    id BINARY(16) NOT NULL PRIMARY KEY,
    dataset_id BINARY(16) NOT NULL,
    number INT NOT NULL,
    frozen_at DATETIME(6) NULL,
    checksum CHAR(64) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_dataset_versions_dataset_id_number UNIQUE (dataset_id, number),
    CONSTRAINT fk_dataset_versions_datasets FOREIGN KEY (dataset_id) REFERENCES datasets (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE dataset_tables (
    id BINARY(16) NOT NULL PRIMARY KEY,
    version_id BINARY(16) NOT NULL,
    name VARCHAR(64) NOT NULL,
    position INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_dataset_tables_version_id_name UNIQUE (version_id, name),
    CONSTRAINT fk_dataset_tables_dataset_versions FOREIGN KEY (version_id) REFERENCES dataset_versions (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE dataset_columns (
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
    CONSTRAINT uk_dataset_columns_table_id_name UNIQUE (table_id, name),
    CONSTRAINT fk_dataset_columns_dataset_tables FOREIGN KEY (table_id) REFERENCES dataset_tables (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE dataset_variants (
    id BINARY(16) NOT NULL PRIMARY KEY,
    version_id BINARY(16) NOT NULL,
    name VARCHAR(100) NOT NULL,
    position INT NOT NULL,
    is_public BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_dataset_variants_version_id_name UNIQUE (version_id, name),
    CONSTRAINT fk_dataset_variants_dataset_versions FOREIGN KEY (version_id) REFERENCES dataset_versions (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE dataset_variant_tables (
    id BINARY(16) NOT NULL PRIMARY KEY,
    variant_id BINARY(16) NOT NULL,
    table_name VARCHAR(64) NOT NULL,
    csv LONGTEXT NOT NULL,
    row_count INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_dataset_variant_tables_variant_id_table_name UNIQUE (variant_id, table_name),
    CONSTRAINT fk_dataset_variant_tables_dataset_variants FOREIGN KEY (variant_id) REFERENCES dataset_variants (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

ALTER TABLE problem_revisions
    ADD COLUMN dataset_version_id BINARY(16) NULL,
    ADD CONSTRAINT fk_problem_revisions_dataset_versions FOREIGN KEY (dataset_version_id) REFERENCES dataset_versions (id);

ALTER TABLE testcases
    ADD COLUMN dataset_variant_id BINARY(16) NULL,
    ADD CONSTRAINT fk_testcases_dataset_variants FOREIGN KEY (dataset_variant_id) REFERENCES dataset_variants (id),
    MODIFY description VARCHAR(200) NULL,
    MODIFY is_public BOOLEAN NULL;
