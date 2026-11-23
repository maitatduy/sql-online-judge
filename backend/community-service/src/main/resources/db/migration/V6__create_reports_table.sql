CREATE TABLE reports (
    id BINARY(16) NOT NULL PRIMARY KEY,
    target_type VARCHAR(16) NOT NULL,
    target_id BINARY(16) NOT NULL,
    target_author_id BINARY(16) NOT NULL,
    reporter_id BINARY(16) NOT NULL,
    reason VARCHAR(30) NOT NULL,
    detail VARCHAR(500) NULL,
    status VARCHAR(16) NOT NULL,
    resolution VARCHAR(16) NULL,
    note VARCHAR(1000) NULL,
    resolved_by BINARY(16) NULL,
    resolved_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_reports_target_type_target_id_reporter_id UNIQUE (target_type, target_id, reporter_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_reports_status_created_at ON reports (status, created_at);
CREATE INDEX idx_reports_target_author_id ON reports (target_author_id);
