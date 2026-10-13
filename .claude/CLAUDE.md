# Cấu hình Claude cho SQL Judge

## Tổng quan

SQL Judge là nền tảng học SQL có máy chấm tự động - online judge, contest, cộng đồng và trợ lý AI. Dự án dùng Claude làm agent phát triển với quy trình rõ ràng, các agent chuyên trách và bộ quy tắc code bắt buộc.

- **Backend:** Java 21, Spring Boot 3 microservices, MySQL 8 - mỗi service một database, Kafka, Redis.
- **Frontend:** một app Next.js 15 (App Router) cho cả site, workspace làm bài và trang admin.
- **Máy chấm:** `judge-worker` chạy SQL người dùng trong sandbox PostgreSQL 16 / MySQL 8 / SQLite 3 tách biệt hoàn toàn.
- **Thiết kế giao diện:** canvas https://claude.ai/artifact/S8V1RwGzroBVtYrCi1HYhZ - mỗi màn hình có mã (6.1 → 6.18, A1 → A16), dùng mã này khi trao đổi.

Package Java gốc: `com.sqljudge.<service>`. Chi tiết stack: `.claude/rules/tech-stack.md`. Cấu trúc repo: `.claude/rules/project-structure.md`.

---

## Quy trình phát triển

Mọi tính năng đi theo quy trình:

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                                                                                  │
│  /spec → /plan-feature → /build-slice → /test-feature → /review-code → /deploy   │
│                                                                                  │
│  Đặc tả   Lập kế hoạch    Thi công       Kiểm chứng      Review        Phát hành │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

| Giai đoạn        | Lệnh            | Mục đích                                                            |
| ---------------- | --------------- | ------------------------------------------------------------------- |
| **Đặc tả**       | `/spec`         | Viết đặc tả: mục tiêu, phạm vi, tiêu chí chấp nhận, ranh giới       |
| **Lập kế hoạch** | `/plan-feature` | Thiết kế backend/frontend, chia lát cắt dọc kèm tiêu chí "xong khi" |
| **Thi công**     | `/build-slice`  | Làm từng lát cắt theo TDD (ĐỎ → XANH → LÀM GỌN)                     |
| **Kiểm chứng**   | `/test-feature` | Viết và chạy test; sửa lỗi theo cách Prove-It                       |
| **Review**       | `/review-code`  | Review 5 trục trước khi merge                                       |
| **Phát hành**    | `/deploy`       | Kiểm tra, build, deploy staging → production, rollback khi lỗi      |

Đặc tả, kế hoạch và danh sách task được trình bày trong cuộc trò chuyện để người dùng duyệt - **không tạo thư mục tài liệu riêng trong repo**. Tiến độ xem qua git log và PR.

### Lệnh hỗ trợ

| Lệnh             | Mục đích                                  |
| ---------------- | ----------------------------------------- |
| `/diagnose`      | Tìm lỗi có hệ thống, truy nguyên nhân gốc |
| `/simplify-code` | Giảm độ phức tạp mà không đổi hành vi     |
| `/fix-issue`     | Phân tích và sửa một lỗi được báo         |

Tên lệnh được đặt khác các lệnh có sẵn của Claude Code (`/plan`, `/review`, `/code-review`, `/debug`, `/security-review`) để không bị đè.

---

## Nguyên tắc cốt lõi

### Chất lượng code

- **Test trước** - viết test đỏ trước, rồi mới code
- **Thi công tăng dần** - lát cắt dọc nhỏ, lúc nào cũng build được
- **Review 5 trục** - Đúng, Dễ đọc, Kiến trúc, Bảo mật, Hiệu năng

### Triết lý

- Tiến bộ hơn hoàn hảo
- Sửa nguyên nhân gốc, không vá triệu chứng
- Thứ đơn giản nhất có thể chạy được
- Test là bằng chứng, không phải việc làm sau

### Quy tắc số 1 - máy chấm chạy code không tin cậy

SQL người dùng gửi lên là **mã độc tiềm năng**. Không bao giờ chạy nó trên DB ứng dụng; chỉ chạy trong sandbox với tài khoản quyền tối thiểu, schema tạm, giới hạn thời gian/số dòng, không có mạng. Không bao giờ lộ testcase ẩn hay đáp án chuẩn ra response, log hoặc câu trả lời của AI. Chi tiết bắt buộc: `judge-sandbox.md`.

---

## Quy tắc bắt buộc

Mọi file trong `.claude/rules/` là **bắt buộc** và phải tuân theo:

### Chất lượng code

| Quy tắc             | Nội dung                                             |
| ------------------- | ---------------------------------------------------- |
| `clean-code.md`     | Biến, hàm, SOLID, xử lý bất đồng bộ                  |
| `code-style.md`     | Định dạng (Spotless, Prettier), quy ước đặt tên      |
| `error-handling.md` | `ApiError`, exception miền, `GlobalExceptionHandler` |

### Kiến trúc và thiết kế

| Quy tắc                | Nội dung                                                           |
| ---------------------- | ------------------------------------------------------------------ |
| `tech-stack.md`        | Công nghệ được duyệt (Spring Boot 3, MySQL, Kafka, Redis, Next.js) |
| `system-design.md`     | Ranh giới service, outbox, cache, scale, hàng đợi chấm             |
| `project-structure.md` | Phân tầng backend, cấu trúc `features/` frontend                   |
| `api-conventions.md`   | Chuẩn REST, `ApiError`, phân trang                                 |
| `design-system.md`     | Token màu, chữ, khoảng cách, màu verdict, nguyên tắc giao diện     |

### Dữ liệu và đặt tên

| Quy tắc                 | Nội dung                                          |
| ----------------------- | ------------------------------------------------- |
| `naming-conventions.md` | Topic Kafka, key Redis, bảng/cột, biến môi trường |
| `database.md`           | Flyway, JPA, UUIDv7, transaction, tránh N+1       |

### Vận hành

| Quy tắc            | Nội dung                                                  |
| ------------------ | --------------------------------------------------------- |
| `judge-sandbox.md` | **NGHIÊM NGẶT** - cô lập sandbox, giới hạn, giấu testcase |
| `security.md`      | **NGHIÊM NGẶT** - không bao giờ vi phạm quy tắc bảo mật   |
| `monitoring.md`    | Prometheus, Grafana, Loki, log, cảnh báo                  |
| `testing.md`       | Ngưỡng coverage, mẫu test                                 |
| `git-workflow.md`  | Chiến lược nhánh, Conventional Commits, không tự push     |

---

## Agent có sẵn

Gọi đúng agent cho từng loại việc:

### Agent phát triển

| Agent                   | Khi nào gọi                                                             |
| ----------------------- | ----------------------------------------------------------------------- |
| **`frontend`**          | Trang, component, routing, state, workspace Monaco, hiệu năng giao diện |
| **`backend`**           | API, service, truy vấn DB, Kafka, logic `judge-worker`                  |
| **`systems-architect`** | Quyết định kiến trúc, ranh giới service, thiết kế event                 |

### Agent chất lượng

| Agent                  | Khi nào gọi                                            |
| ---------------------- | ------------------------------------------------------ |
| **`code-reviewer`**    | Review PR 5 trục, đánh giá chất lượng code             |
| **`test-engineer`**    | Chiến lược test, TDD, coverage, tái hiện lỗi           |
| **`security-auditor`** | Đánh giá lỗ hổng, mô hình mối đe doạ, sandbox chấm bài |
| **`qa`**               | Test plan, test E2E, báo cáo lỗi                       |

### Agent sản phẩm

| Agent                 | Khi nào gọi                                                  |
| --------------------- | ------------------------------------------------------------ |
| **`project-manager`** | User story, lộ trình, kế hoạch sprint, báo cáo tiến độ       |
| **`ui-ux-designer`**  | Design system, màn hình mới, design brief, khả năng truy cập |
| **`copywriter-seo`**  | Chữ trên trang, meta tag, SEO tiếng Việt                     |

---

## Skill có sẵn

Skill chuyên biệt cho các việc phức tạp:

| Skill                        | Nội dung                                                 |
| ---------------------------- | -------------------------------------------------------- |
| `tdd`                        | Mẫu phát triển hướng test                                |
| `five-axis-review`           | Khung review 5 trục                                      |
| `incremental-implementation` | Thi công theo lát cắt dọc                                |
| `deploy`                     | Quy trình triển khai đầy đủ (chỉ chạy khi gọi `/deploy`) |
| `security-audit`             | Checklist rà soát bảo mật                                |

---

## Checklist tham chiếu

Tra nhanh trong `.claude/references/`:

| Tài liệu                     | Dùng cho                                    |
| ---------------------------- | ------------------------------------------- |
| `security-checklist.md`      | Kiểm tra bảo mật trước khi deploy           |
| `testing-patterns.md`        | Cấu trúc test và các lỗi thường gặp         |
| `performance-checklist.md`   | Core Web Vitals, tối ưu backend và frontend |
| `accessibility-checklist.md` | Tuân thủ WCAG 2.1 AA                        |

---

## Lệnh thường dùng

```bash
docker compose -f infra/docker-compose.dev.yml up -d   # MySQL, Kafka, Redis, sandbox
./mvnw spring-boot:run | ./mvnw test | ./mvnw verify    # trong backend/<service>
npm run dev | npm run format | npm run lint && npm run typecheck | npm run test | npm run test:e2e   # trong frontend/
```

Nếu lệnh thật trong repo khác danh sách này, tin repo và cập nhật lại mục này.

---

## Từ vựng miền nghiệp vụ

- **Verdict:** `AC` đúng · `WA` sai dữ liệu · `OM` đúng dữ liệu sai thứ tự · `TLE` quá thời gian · `RE` lỗi cú pháp/thực thi · `FS` dùng lệnh bị cấm · `SE` lỗi hạ tầng chấm (chỉ admin thấy, tự chấm lại).
- **Vai trò:** `LEARNER`, `SETTER`, `MODERATOR`, `ADMIN`.
- **Độ khó:** `EASY`, `MEDIUM`, `HARD`. **Dialect:** `POSTGRESQL`, `MYSQL`, `SQLITE`.

---

## Hướng dẫn hành vi của agent

1. **Theo quy trình** - `/spec` → `/plan-feature` → `/build-slice` → `/review-code`
2. **Áp dụng quy tắc bắt buộc** - mọi file trong `.claude/rules/` là không thương lượng
3. **Test trước** - viết test đỏ trước khi code
4. **Thay đổi nhỏ** - commit nhỏ, lúc nào cũng build được
5. **Giải thích trước khi làm** - mô tả thay đổi trước khi thực hiện
6. **Sửa nguyên nhân gốc** - không vá triệu chứng
7. **Dùng đúng agent** - giao việc cho agent chuyên trách theo lĩnh vực
8. **Không động vào bí mật** - không đọc, không commit `.env`, `application-secret.yml`, `application-prod.yml`, khoá, chứng chỉ
9. **Hỏi trước khi push** - không bao giờ force push, không push khi người dùng chưa đồng ý
10. **Trả lời bằng tiếng Việt** - code, tên biến, commit message bằng tiếng Anh
