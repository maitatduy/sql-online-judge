CREATE TABLE problem_stats (
    id BINARY(16) NOT NULL PRIMARY KEY,
    problem_id BINARY(16) NOT NULL,
    submission_count BIGINT NOT NULL,
    accepted_count BIGINT NOT NULL,
    solved_user_count BIGINT NOT NULL,
    version BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_problem_stats_problem_id UNIQUE (problem_id),
    CONSTRAINT fk_problem_stats_problems FOREIGN KEY (problem_id) REFERENCES problems (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE user_problem_progress (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    problem_id BINARY(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    first_solved_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_user_problem_progress_user_id_problem_id UNIQUE (user_id, problem_id),
    CONSTRAINT fk_user_problem_progress_problems FOREIGN KEY (problem_id) REFERENCES problems (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_user_problem_progress_user_id_status ON user_problem_progress (user_id, status);
