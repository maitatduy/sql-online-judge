CREATE TABLE lesson_runs (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    cell_key BINARY(16) NOT NULL,
    dialect VARCHAR(20) NOT NULL,
    sql_text MEDIUMTEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    outcome VARCHAR(8) NULL,
    result_json JSON NULL,
    error_message VARCHAR(2000) NULL,
    runtime_ms INT NULL,
    completed_at DATETIME(6) NULL,
    version BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_lesson_runs_user_id_created_at ON lesson_runs (user_id, created_at);

CREATE TABLE outbox_events (
    id BINARY(16) NOT NULL PRIMARY KEY,
    topic VARCHAR(200) NOT NULL,
    message_key VARCHAR(100) NOT NULL,
    payload LONGTEXT NOT NULL,
    claimed_at DATETIME(6) NULL,
    published_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_outbox_events_published_at_created_at ON outbox_events (published_at, created_at);
