CREATE TABLE contest_standings (
    id BINARY(16) NOT NULL PRIMARY KEY,
    contest_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    username VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NULL,
    rank_position INT NOT NULL,
    solved INT NOT NULL,
    penalty_minutes BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT fk_contest_standings_contests FOREIGN KEY (contest_id) REFERENCES contests (id),
    CONSTRAINT uk_contest_standings_contest_id_user_id UNIQUE (contest_id, user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_contest_standings_contest_id_rank_position
    ON contest_standings (contest_id, rank_position);
