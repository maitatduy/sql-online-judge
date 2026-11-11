CREATE TABLE processed_events (
    id BINARY(16) NOT NULL PRIMARY KEY,
    event_id BINARY(16) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_processed_events_event_id UNIQUE (event_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
