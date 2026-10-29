CREATE TABLE tags (
    id BINARY(16) NOT NULL PRIMARY KEY,
    slug VARCHAR(60) NOT NULL,
    name VARCHAR(60) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_tags_slug UNIQUE (slug),
    CONSTRAINT uk_tags_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
