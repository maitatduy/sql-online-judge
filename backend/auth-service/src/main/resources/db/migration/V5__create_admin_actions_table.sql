CREATE TABLE admin_actions (
    id BINARY(16) NOT NULL PRIMARY KEY,
    actor_id BINARY(16) NOT NULL,
    target_user_id BINARY(16) NOT NULL,
    action VARCHAR(20) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    expires_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT fk_admin_actions_users_actor FOREIGN KEY (actor_id) REFERENCES users (id),
    CONSTRAINT fk_admin_actions_users_target FOREIGN KEY (target_user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_admin_actions_target_user_id_created_at ON admin_actions (target_user_id, created_at);
