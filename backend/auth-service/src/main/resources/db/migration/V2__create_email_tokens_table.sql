CREATE TABLE email_tokens (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    purpose VARCHAR(10) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    code_hash CHAR(64) NULL,
    failed_attempts INT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_email_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_email_tokens_users FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_email_tokens_user_id_purpose ON email_tokens (user_id, purpose);
