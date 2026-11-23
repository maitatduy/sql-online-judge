CREATE TABLE comments (
    id BINARY(16) NOT NULL PRIMARY KEY,
    post_id BINARY(16) NULL,
    problem_id BINARY(16) NULL,
    parent_id BINARY(16) NULL,
    author_id BINARY(16) NOT NULL,
    body TEXT NOT NULL,
    vote_score INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    deleted_at DATETIME(6) NULL,
    version BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_comments_post_id_created_at ON comments (post_id, created_at);
CREATE INDEX idx_comments_problem_id_created_at ON comments (problem_id, created_at);
CREATE INDEX idx_comments_parent_id ON comments (parent_id);
