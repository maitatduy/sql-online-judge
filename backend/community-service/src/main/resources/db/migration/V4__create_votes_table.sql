CREATE TABLE votes (
    id BINARY(16) NOT NULL PRIMARY KEY,
    target_type VARCHAR(16) NOT NULL,
    target_id BINARY(16) NOT NULL,
    target_author_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    vote_value INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_votes_target_type_target_id_user_id UNIQUE (target_type, target_id, user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_votes_target_author_id_updated_at ON votes (target_author_id, updated_at);
