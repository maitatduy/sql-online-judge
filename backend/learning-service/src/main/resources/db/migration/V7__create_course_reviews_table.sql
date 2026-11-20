CREATE TABLE course_reviews (
    id BINARY(16) NOT NULL PRIMARY KEY,
    course_id BINARY(16) NOT NULL,
    revision_id BINARY(16) NOT NULL,
    reviewer_id BINARY(16) NOT NULL,
    decision VARCHAR(20) NOT NULL,
    reason TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT fk_course_reviews_courses FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT fk_course_reviews_course_revisions FOREIGN KEY (revision_id) REFERENCES course_revisions (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_course_reviews_course_id_created_at ON course_reviews (course_id, created_at);
