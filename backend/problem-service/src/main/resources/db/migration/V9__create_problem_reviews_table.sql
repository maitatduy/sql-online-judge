CREATE TABLE problem_reviews (
    id BINARY(16) NOT NULL PRIMARY KEY,
    problem_id BINARY(16) NOT NULL,
    revision_id BINARY(16) NOT NULL,
    reviewer_id BINARY(16) NOT NULL,
    decision VARCHAR(20) NOT NULL,
    reason TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT fk_problem_reviews_problems FOREIGN KEY (problem_id) REFERENCES problems (id) ON DELETE CASCADE,
    CONSTRAINT fk_problem_reviews_problem_revisions FOREIGN KEY (revision_id) REFERENCES problem_revisions (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_problem_reviews_problem_id_created_at ON problem_reviews (problem_id, created_at);
