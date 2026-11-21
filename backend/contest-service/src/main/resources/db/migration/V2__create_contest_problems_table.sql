CREATE TABLE contest_problems (
    id BINARY(16) NOT NULL PRIMARY KEY,
    contest_id BINARY(16) NOT NULL,
    problem_id BINARY(16) NOT NULL,
    position INT NOT NULL,
    slug VARCHAR(160) NOT NULL,
    title VARCHAR(200) NOT NULL,
    difficulty VARCHAR(10) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT fk_contest_problems_contests FOREIGN KEY (contest_id) REFERENCES contests (id),
    CONSTRAINT uk_contest_problems_contest_id_problem_id UNIQUE (contest_id, problem_id),
    CONSTRAINT uk_contest_problems_contest_id_position UNIQUE (contest_id, position)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
