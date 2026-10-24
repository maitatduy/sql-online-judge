CREATE TABLE admin_user_notes (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    author_id BINARY(16) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT fk_admin_user_notes_users FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_admin_user_notes_users_author FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_admin_user_notes_user_id_created_at ON admin_user_notes (user_id, created_at);
