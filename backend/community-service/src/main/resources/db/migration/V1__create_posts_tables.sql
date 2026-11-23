CREATE TABLE posts (
    id BINARY(16) NOT NULL PRIMARY KEY,
    author_id BINARY(16) NOT NULL,
    category VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body_markdown MEDIUMTEXT NOT NULL,
    problem_id BINARY(16) NULL,
    status VARCHAR(16) NOT NULL,
    is_pinned BOOLEAN NOT NULL,
    vote_score INT NOT NULL,
    comment_count INT NOT NULL,
    view_count BIGINT NOT NULL,
    published_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    version BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_posts_status_published_at ON posts (status, published_at);
CREATE INDEX idx_posts_category_status_published_at ON posts (category, status, published_at);
CREATE INDEX idx_posts_problem_id_category ON posts (problem_id, category);
CREATE INDEX idx_posts_author_id ON posts (author_id);

CREATE TABLE post_tags (
    post_id BINARY(16) NOT NULL,
    tag VARCHAR(40) NOT NULL,
    PRIMARY KEY (post_id, tag),
    CONSTRAINT fk_post_tags_posts FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_post_tags_tag ON post_tags (tag);
