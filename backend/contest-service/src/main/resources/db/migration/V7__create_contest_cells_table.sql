CREATE TABLE contest_cells (
    id BINARY(16) NOT NULL PRIMARY KEY,
    contest_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    problem_id BINARY(16) NOT NULL,
    accepted_minute INT NULL,
    wrong_attempts INT NOT NULL,
    penalty_minutes INT NOT NULL,
    public_accepted_minute INT NULL,
    public_wrong_attempts INT NOT NULL,
    public_penalty_minutes INT NOT NULL,
    pending_attempts INT NOT NULL,
    version BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT fk_contest_cells_contests FOREIGN KEY (contest_id) REFERENCES contests (id),
    CONSTRAINT uk_contest_cells_contest_id_user_id_problem_id
        UNIQUE (contest_id, user_id, problem_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
