CREATE TABLE lesson_cells (
    id BINARY(16) NOT NULL PRIMARY KEY,
    lesson_id BINARY(16) NOT NULL,
    cell_key BINARY(16) NOT NULL,
    position INT NOT NULL,
    starter_sql TEXT NOT NULL,
    dialect VARCHAR(20) NOT NULL,
    dataset_version_id BINARY(16) NOT NULL,
    dataset_variant_id BINARY(16) NOT NULL,
    dataset_checksum VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT fk_lesson_cells_lessons FOREIGN KEY (lesson_id) REFERENCES lessons (id),
    CONSTRAINT uk_lesson_cells_lesson_id_position UNIQUE (lesson_id, position)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_lesson_cells_cell_key ON lesson_cells (cell_key);
