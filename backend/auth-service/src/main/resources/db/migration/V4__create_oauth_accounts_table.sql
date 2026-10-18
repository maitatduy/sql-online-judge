CREATE TABLE oauth_accounts (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    provider VARCHAR(10) NOT NULL,
    provider_user_id VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_oauth_accounts_provider_provider_user_id UNIQUE (provider, provider_user_id),
    CONSTRAINT fk_oauth_accounts_users FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
