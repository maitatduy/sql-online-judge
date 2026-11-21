CREATE TABLE contests (
    id BINARY(16) NOT NULL PRIMARY KEY,
    slug VARCHAR(160) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    start_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    duration_minutes INT NOT NULL,
    freeze_minutes INT NOT NULL,
    penalty_minutes INT NOT NULL,
    is_rated BOOLEAN NOT NULL,
    version BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_contests_slug UNIQUE (slug)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_contests_start_at ON contests (start_at);
CREATE INDEX idx_contests_end_at ON contests (end_at);
