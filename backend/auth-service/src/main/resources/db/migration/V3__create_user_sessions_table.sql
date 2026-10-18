CREATE TABLE user_sessions (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    previous_token_hash CHAR(64) NULL,
    rotated_at DATETIME(6) NULL,
    is_persistent BOOLEAN NOT NULL,
    user_agent VARCHAR(255) NULL,
    expires_at DATETIME(6) NOT NULL,
    last_used_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_user_sessions_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_user_sessions_users FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_user_sessions_previous_token_hash ON user_sessions (previous_token_hash);

CREATE INDEX idx_user_sessions_user_id_revoked_at ON user_sessions (user_id, revoked_at);
