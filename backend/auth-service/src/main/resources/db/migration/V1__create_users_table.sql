CREATE TABLE users (
    id BINARY(16) NOT NULL PRIMARY KEY,
    username VARCHAR(30) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash CHAR(60) NULL,
    role VARCHAR(20) NOT NULL,
    display_name VARCHAR(100) NULL,
    signup_provider VARCHAR(10) NOT NULL,
    email_verified_at DATETIME(6) NULL,
    banned_at DATETIME(6) NULL,
    banned_until DATETIME(6) NULL,
    ban_reason VARCHAR(500) NULL,
    last_active_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    version BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_users_role ON users (role);

CREATE INDEX idx_users_created_at ON users (created_at);
