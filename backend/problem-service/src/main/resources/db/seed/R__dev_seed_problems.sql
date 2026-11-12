UPDATE problems SET draft_revision_id = NULL, published_revision_id = NULL
WHERE id = UUID_TO_BIN('01a1c5b1-f3a3-7801-8465-41ddf7d6d2cd');

DELETE FROM problems WHERE id = UUID_TO_BIN('01a1c5b1-f3a3-7801-8465-41ddf7d6d2cd');

DELETE FROM datasets WHERE id = UUID_TO_BIN('01a1c5ad-9547-78d4-b826-77a638cb9597');

INSERT INTO tags (id, slug, name, created_at, updated_at) VALUES
    (UUID_TO_BIN('0192f3b2-0000-7000-8000-000000000001'), 'select-where', 'SELECT/WHERE', '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000'),
    (UUID_TO_BIN('0192f3b2-0000-7000-8000-000000000002'), 'join', 'JOIN', '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000'),
    (UUID_TO_BIN('0192f3b2-0000-7000-8000-000000000003'), 'group-by', 'GROUP BY/HAVING', '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000'),
    (UUID_TO_BIN('0192f3b2-0000-7000-8000-000000000004'), 'subquery', 'Subquery', '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000'),
    (UUID_TO_BIN('0192f3b2-0000-7000-8000-000000000005'), 'cte', 'CTE', '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000'),
    (UUID_TO_BIN('0192f3b2-0000-7000-8000-000000000006'), 'window-function', 'Window Function', '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000'),
    (UUID_TO_BIN('0192f3b2-0000-7000-8000-000000000007'), 'null', 'NULL', '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000'),
    (UUID_TO_BIN('0192f3b2-0000-7000-8000-000000000008'), 'date-time', 'Ngày giờ', '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000')
AS seed ON DUPLICATE KEY UPDATE slug = seed.slug, name = seed.name;

INSERT INTO datasets (id, owner_id, slug, name, description, version, created_at, updated_at) VALUES
    (UUID_TO_BIN('01a1c5ad-9547-78d4-b826-77a638cb9597'), UUID_TO_BIN('01a149ad-e81e-725d-b6f2-6159a06e0f37'), 'cong-ty', 'Công ty',
     'Phòng ban và nhân viên kèm lương', 0, '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000');

INSERT INTO dataset_versions (id, dataset_id, number, checksum, frozen_at, created_at, updated_at) VALUES
    (UUID_TO_BIN('01a1c5ad-9556-7005-8a9e-d419c2a1a337'), UUID_TO_BIN('01a1c5ad-9547-78d4-b826-77a638cb9597'), 1, '864036c493100384e854c43789114c48648c42dbb65407b37634bed267c4d513', '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000');

INSERT INTO dataset_tables (id, version_id, name, position, created_at, updated_at) VALUES
    (UUID_TO_BIN('01a1c5ae-5bc3-7382-8e11-bc2abb9394ca'), UUID_TO_BIN('01a1c5ad-9556-7005-8a9e-d419c2a1a337'), 'departments', 0, '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000'),
    (UUID_TO_BIN('01a1c5ae-5bd5-71d3-b2dd-d8597fa28473'), UUID_TO_BIN('01a1c5ad-9556-7005-8a9e-d419c2a1a337'), 'employees', 1, '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000');

INSERT INTO dataset_columns (id, table_id, name, logical_type, is_primary_key, is_nullable, references_table,
    references_column, position, created_at, updated_at) VALUES
    (UUID_TO_BIN('01a1c5ae-5bc9-7e6c-bf49-1f3d9428b959'), UUID_TO_BIN('01a1c5ae-5bc3-7382-8e11-bc2abb9394ca'), 'id', 'INTEGER', TRUE, FALSE,
     NULL, NULL, 0, '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000'),
    (UUID_TO_BIN('01a1c5ae-5bd4-72b1-8884-e6ff210d2cbe'), UUID_TO_BIN('01a1c5ae-5bc3-7382-8e11-bc2abb9394ca'), 'name', 'TEXT', FALSE, FALSE,
     NULL, NULL, 1, '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000'),
    (UUID_TO_BIN('01a1c5ae-5bd5-74b2-aae1-1abb7a12fba4'), UUID_TO_BIN('01a1c5ae-5bd5-71d3-b2dd-d8597fa28473'), 'id', 'INTEGER', TRUE, FALSE,
     NULL, NULL, 0, '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000'),
    (UUID_TO_BIN('01a1c5ae-5bd6-7ae0-a32f-e2a96d39b512'), UUID_TO_BIN('01a1c5ae-5bd5-71d3-b2dd-d8597fa28473'), 'name', 'TEXT', FALSE, FALSE,
     NULL, NULL, 1, '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000'),
    (UUID_TO_BIN('01a1c5ae-5bd8-734c-a17e-fb95111dbe29'), UUID_TO_BIN('01a1c5ae-5bd5-71d3-b2dd-d8597fa28473'), 'department_id', 'INTEGER', FALSE, TRUE,
     'departments', 'id', 2, '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000'),
    (UUID_TO_BIN('01a1c5ae-5bd8-7ba0-9de6-c3db2bb387c4'), UUID_TO_BIN('01a1c5ae-5bd5-71d3-b2dd-d8597fa28473'), 'salary', 'DECIMAL', FALSE, TRUE,
     NULL, NULL, 3, '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000');

INSERT INTO dataset_variants (id, version_id, name, position, is_public, created_at, updated_at) VALUES
    (UUID_TO_BIN('01a1c5ae-5c79-705f-a0dd-596698018306'), UUID_TO_BIN('01a1c5ad-9556-7005-8a9e-d419c2a1a337'), 'Mẫu', 0, TRUE, '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000'),
    (UUID_TO_BIN('01a1c5ae-95f7-710a-aa81-3276ccbc9ba3'), UUID_TO_BIN('01a1c5ad-9556-7005-8a9e-d419c2a1a337'), 'Có NULL', 1, FALSE, '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000');

INSERT INTO dataset_variant_tables (id, variant_id, table_name, csv, row_count, created_at, updated_at) VALUES
    (UUID_TO_BIN('01a1c5ae-5cf9-744e-a977-d1be0680d020'), UUID_TO_BIN('01a1c5ae-5c79-705f-a0dd-596698018306'), 'departments',
     'id,name\n1,Kinh doanh\n2,Kỹ thuật\n3,Nhân sự\n', 3, '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000'),
    (UUID_TO_BIN('01a1c5ae-5d4e-736f-ace5-8df6b7711d5d'), UUID_TO_BIN('01a1c5ae-5c79-705f-a0dd-596698018306'), 'employees',
     'id,name,department_id,salary\n1,An,1,12000000\n2,Bình,1,15000000\n3,Chi,2,20000000\n4,Dũng,2,25000000\n5,Em,3,9000000\n', 5, '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000'),
    (UUID_TO_BIN('01a1c5b0-4c6d-7d1d-b6b5-8b6246fc59b7'), UUID_TO_BIN('01a1c5ae-95f7-710a-aa81-3276ccbc9ba3'), 'departments',
     'id,name\n1,Kinh doanh\n2,Kỹ thuật\n', 2, '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000'),
    (UUID_TO_BIN('01a1c5b0-4cb9-7a6f-bce0-9774a39b0757'), UUID_TO_BIN('01a1c5ae-95f7-710a-aa81-3276ccbc9ba3'), 'employees',
     'id,name,department_id,salary\n1,An,1,10000000\n2,Bình,1,\n3,Chi,2,30000000\n', 3, '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000');

INSERT INTO problems (id, slug, number, title, difficulty, status, visibility, author_id, statement_markdown,
    starter_code, time_limit_ms, requires_order, compare_column_names, float_tolerance, allowed_statements,
    published_at, version, created_at, updated_at) VALUES
    (UUID_TO_BIN('01a1c5b1-f3a3-7801-8465-41ddf7d6d2cd'), 'tong-luong-moi-phong-ban', 1, 'Tổng lương mỗi phòng ban', 'EASY', 'PUBLISHED', 'PUBLIC',
     UUID_TO_BIN('01a149ad-e81e-725d-b6f2-6159a06e0f37'),
     'Cho hai bảng `departments` (phòng ban) và `employees` (nhân viên).\n\nVới mỗi phòng ban có nhân viên, in ra:\n\n- `department`: tên phòng ban\n- `total_salary`: tổng lương của nhân viên trong phòng ban, bỏ qua lương NULL\n\nSắp xếp kết quả theo `department` tăng dần.\n',
     '', 2000, TRUE, TRUE, 0.000001, 'SELECT,WITH', '2026-10-01 00:00:00.000000', 0, '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000');

INSERT INTO problem_revisions (id, problem_id, number, frozen_at, dataset_version_id, created_at, updated_at) VALUES
    (UUID_TO_BIN('01a1c5b1-f3a6-7455-990a-f4dc332b9831'), UUID_TO_BIN('01a1c5b1-f3a3-7801-8465-41ddf7d6d2cd'), 1, '2026-10-01 00:00:00.000000',
     UUID_TO_BIN('01a1c5ad-9556-7005-8a9e-d419c2a1a337'), '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000');

UPDATE problems
SET draft_revision_id = UUID_TO_BIN('01a1c5b1-f3a6-7455-990a-f4dc332b9831'),
    published_revision_id = UUID_TO_BIN('01a1c5b1-f3a6-7455-990a-f4dc332b9831')
WHERE id = UUID_TO_BIN('01a1c5b1-f3a3-7801-8465-41ddf7d6d2cd');

INSERT INTO problem_tags (problem_id, tag_id) VALUES
    (UUID_TO_BIN('01a1c5b1-f3a3-7801-8465-41ddf7d6d2cd'), UUID_TO_BIN('0192f3b2-0000-7000-8000-000000000003'));

INSERT INTO reference_solutions (id, revision_id, dialect, code, created_at, updated_at) VALUES
    (UUID_TO_BIN('01a1c5ba-0c16-72a0-ae88-f9e6cac8ab88'), UUID_TO_BIN('01a1c5b1-f3a6-7455-990a-f4dc332b9831'), 'POSTGRESQL',
     'SELECT d.name AS department, SUM(e.salary) AS total_salary\nFROM departments d\nJOIN employees e ON e.department_id = d.id\nGROUP BY d.name\nORDER BY d.name;\n', '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000'),
    (UUID_TO_BIN('01a1c5ba-0c1a-7152-9ca2-6cd4af1a759d'), UUID_TO_BIN('01a1c5b1-f3a6-7455-990a-f4dc332b9831'), 'MYSQL',
     'SELECT d.name AS department, SUM(e.salary) AS total_salary\nFROM departments d\nJOIN employees e ON e.department_id = d.id\nGROUP BY d.name\nORDER BY d.name;\n', '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000'),
    (UUID_TO_BIN('01a1c5ba-0c1b-70a3-93c5-4f9101186b77'), UUID_TO_BIN('01a1c5b1-f3a6-7455-990a-f4dc332b9831'), 'SQLITE',
     'SELECT d.name AS department, SUM(e.salary) AS total_salary\nFROM departments d\nJOIN employees e ON e.department_id = d.id\nGROUP BY d.name\nORDER BY d.name;\n', '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000');

INSERT INTO testcases (id, revision_id, position, dataset_variant_id, created_at, updated_at) VALUES
    (UUID_TO_BIN('01a1c5ba-0c1b-7872-a269-8b71525e1b69'), UUID_TO_BIN('01a1c5b1-f3a6-7455-990a-f4dc332b9831'), 0, UUID_TO_BIN('01a1c5ae-5c79-705f-a0dd-596698018306'), '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000'),
    (UUID_TO_BIN('01a1c5ba-0c20-7629-838d-71d327538399'), UUID_TO_BIN('01a1c5b1-f3a6-7455-990a-f4dc332b9831'), 1, UUID_TO_BIN('01a1c5ae-95f7-710a-aa81-3276ccbc9ba3'), '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000');

INSERT INTO expected_results (id, testcase_id, columns_json, rows_json, row_count, checksum, created_at, updated_at) VALUES
    (UUID_TO_BIN('01a1c5be-0825-7ea7-a88a-e6fd47d7354d'), UUID_TO_BIN('01a1c5ba-0c1b-7872-a269-8b71525e1b69'), '["department", "total_salary"]',
     '[[{"kind":"TEXT","value":"Kinh doanh"},{"kind":"NUMBER","value":"27000000"}],[{"kind":"TEXT","value":"Kỹ thuật"},{"kind":"NUMBER","value":"45000000"}],[{"kind":"TEXT","value":"Nhân sự"},{"kind":"NUMBER","value":"9000000"}]]', 3, '7aa4374ea568d37f148cdeb5e49343b3df90f37a637bc6ea5b176a8b238281aa', '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000'),
    (UUID_TO_BIN('01a1c5be-0b2b-7715-9660-eb83b6866125'), UUID_TO_BIN('01a1c5ba-0c20-7629-838d-71d327538399'), '["department", "total_salary"]',
     '[[{"kind":"TEXT","value":"Kinh doanh"},{"kind":"NUMBER","value":"10000000"}],[{"kind":"TEXT","value":"Kỹ thuật"},{"kind":"NUMBER","value":"30000000"}]]', 2, 'e4bfbe5a7b847db44f5611c76419774e23d5cd50e70a4da28e53ca0990a5ea56', '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000');

INSERT INTO problem_stats (id, problem_id, submission_count, accepted_count, solved_user_count, version, created_at, updated_at) VALUES
    (UUID_TO_BIN('01a1cd3b-44ac-7763-a180-ffee5348c40e'), UUID_TO_BIN('01a1c5b1-f3a3-7801-8465-41ddf7d6d2cd'), 0, 0, 0, 0, '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000');
