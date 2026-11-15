DELETE FROM lessons WHERE chapter_id IN (UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000011'), UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000012'));
DELETE FROM chapters WHERE revision_id = UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000002');
DELETE FROM course_revisions WHERE id = UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000002');

INSERT INTO courses (id, slug, author_id, status, published_revision_id, draft_revision_id, deleted_at, version, created_at, updated_at)
VALUES (UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000001'), 'sql-co-ban', UUID_TO_BIN('0192f3a0-0000-7000-8000-000000000009'), 'PUBLISHED', UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000002'), NULL, NULL, 0, '2026-11-01 08:00:00', '2026-11-01 08:00:00')
ON DUPLICATE KEY UPDATE slug = VALUES(slug), author_id = VALUES(author_id), status = VALUES(status), published_revision_id = VALUES(published_revision_id), draft_revision_id = VALUES(draft_revision_id), deleted_at = VALUES(deleted_at);

INSERT INTO course_revisions (id, course_id, status, title, summary, difficulty, estimated_minutes, lesson_count, submitted_at, published_at, created_at, updated_at)
VALUES (UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000002'), UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000001'), 'PUBLISHED', 'SQL cơ bản', 'Chọn cột, lọc dòng và sắp xếp kết quả.', 'EASY', 40, 4, '2026-11-01 07:00:00', '2026-11-01 08:00:00', '2026-11-01 08:00:00', '2026-11-01 08:00:00');

INSERT INTO chapters (id, revision_id, title, position, created_at, updated_at) VALUES
(UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000011'), UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000002'), 'Làm quen với SELECT', 1, '2026-11-01 08:00:00', '2026-11-01 08:00:00'),
(UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000012'), UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000002'), 'Lọc và sắp xếp', 2, '2026-11-01 08:00:00', '2026-11-01 08:00:00');

INSERT INTO lessons (id, chapter_id, lesson_key, slug, title, body, position, created_at, updated_at) VALUES
(UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000101'), UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000011'), UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000201'), 'select-la-gi', 'SELECT là gì', '## Câu truy vấn đầu tiên\n\n`SELECT` lấy dữ liệu từ một bảng. Câu đơn giản nhất lấy mọi cột của mọi dòng:\n\n```sql\nSELECT * FROM departments;\n```\n\nDấu `*` nghĩa là tất cả các cột. Kết quả là một bảng mới, bảng gốc không thay đổi.', 1, '2026-11-01 08:00:00', '2026-11-01 08:00:00'),
(UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000102'), UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000011'), UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000202'), 'chon-cot', 'Chọn cột và đặt bí danh', '## Chỉ lấy cột cần thiết\n\nLiệt kê tên cột sau `SELECT`, cách nhau bằng dấu phẩy:\n\n```sql\nSELECT name, salary FROM employees;\n```\n\nDùng `AS` để đặt tên khác cho cột trong kết quả:\n\n```sql\nSELECT name AS employee_name FROM employees;\n```', 2, '2026-11-01 08:00:00', '2026-11-01 08:00:00'),
(UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000103'), UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000012'), UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000203'), 'menh-de-where', 'Lọc dòng với WHERE', '## Giữ lại những dòng thoả điều kiện\n\n`WHERE` đặt sau `FROM` và chỉ giữ các dòng có điều kiện đúng:\n\n```sql\nSELECT name, salary FROM employees WHERE salary > 15000000;\n```\n\nGhép nhiều điều kiện bằng `AND` và `OR`.', 1, '2026-11-01 08:00:00', '2026-11-01 08:00:00'),
(UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000104'), UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000012'), UUID_TO_BIN('01a1d2c0-0000-7000-8000-000000000204'), 'order-by', 'Sắp xếp với ORDER BY', '## Sắp xếp kết quả\n\nKhông có `ORDER BY` thì thứ tự dòng không được đảm bảo. Sắp xếp tăng dần là mặc định, thêm `DESC` để giảm dần:\n\n```sql\nSELECT name, salary FROM employees ORDER BY salary DESC;\n```', 2, '2026-11-01 08:00:00', '2026-11-01 08:00:00');
