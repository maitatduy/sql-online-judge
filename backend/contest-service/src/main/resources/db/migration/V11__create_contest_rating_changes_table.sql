CREATE TABLE contest_rating_changes (
    id BINARY(16) NOT NULL PRIMARY KEY,
    contest_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    username VARCHAR(50) NOT NULL,
    rank_position INT NOT NULL,
    old_rating INT NOT NULL,
    new_rating INT NOT NULL,
    delta INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT fk_contest_rating_changes_contests FOREIGN KEY (contest_id) REFERENCES contests (id),
    CONSTRAINT uk_contest_rating_changes_contest_id_user_id UNIQUE (contest_id, user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_contest_rating_changes_contest_id_rank_position
    ON contest_rating_changes (contest_id, rank_position);
