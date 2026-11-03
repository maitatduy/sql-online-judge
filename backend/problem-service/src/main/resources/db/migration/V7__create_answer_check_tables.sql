CREATE TABLE answer_checks (
    id BINARY(16) NOT NULL PRIMARY KEY,
    problem_id BINARY(16) NOT NULL,
    revision_id BINARY(16) NOT NULL,
    dataset_version_id BINARY(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    testcase_count INT NOT NULL,
    completed_at DATETIME(6) NULL,
    version BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT fk_answer_checks_problems FOREIGN KEY (problem_id) REFERENCES problems (id) ON DELETE CASCADE,
    CONSTRAINT fk_answer_checks_problem_revisions FOREIGN KEY (revision_id) REFERENCES problem_revisions (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_answer_checks_problem_id_created_at ON answer_checks (problem_id, created_at);

CREATE TABLE answer_check_testcases (
    id BINARY(16) NOT NULL PRIMARY KEY,
    check_id BINARY(16) NOT NULL,
    testcase_id BINARY(16) NOT NULL,
    position INT NOT NULL,
    is_public BOOLEAN NOT NULL,
    is_passed BOOLEAN NOT NULL,
    system_error VARCHAR(64) NULL,
    dialects_json JSON NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_answer_check_testcases_check_id_testcase_id UNIQUE (check_id, testcase_id),
    CONSTRAINT fk_answer_check_testcases_answer_checks FOREIGN KEY (check_id) REFERENCES answer_checks (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
