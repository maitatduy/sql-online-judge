# Cấu trúc dự án

## Toàn repo

```
sql-online-judge/
├── .claude/                 # cấu hình Claude (CLAUDE.md, rules, agents, commands, skills, references)
├── backend/
│   ├── pom.xml  mvnw  .mvn/ # parent POM (Java 17, Spring Boot 3), Maven wrapper - chạy mọi lệnh Maven ở đây
│   ├── common-lib/
│   ├── auth-service/
│   └── <service>/           # sẽ thêm: api-gateway, discovery-server, config-server, problem, learning,
│                            # submission, judge-worker, contest, community, ai, notification
├── frontend/
├── infra/
│   ├── docker-compose.dev.yml  .env.example   # MySQL, Redis, Mailpit
│   ├── mysql/init/          # tạo database + user riêng cho từng service
│   ├── sandbox/             # sẽ thêm: Dockerfile + cấu hình các DB sandbox
│   └── monitoring/          # sẽ thêm: prometheus.yml, dashboard Grafana, rule cảnh báo
└── .github/workflows/       # sẽ thêm: CI và deploy
```

Không tạo thư mục tài liệu (`docs/`…) trong repo. Đặc tả, kế hoạch, quyết định kiến trúc trình bày trong cuộc trò chuyện và tóm tắt vào mô tả PR.

---

## Một service backend

```
backend/problem-service/
├── pom.xml
└── src/
    ├── main/java/com/sqljudge/problem/
    │   ├── ProblemServiceApplication.java
    │   ├── config/           # SecurityConfig, KafkaConfig, OpenApiConfig
    │   ├── controller/       # chỉ nhận request, gọi service
    │   ├── service/          # business logic, @Transactional đặt ở đây
    │   ├── repository/       # Spring Data JPA
    │   ├── entity/           # JPA entity, kế thừa BaseEntity của common-lib
    │   ├── dto/request/      # record + Bean Validation
    │   ├── dto/response/
    │   ├── mapper/           # MapStruct
    │   ├── client/           # OpenFeign client gọi service khác
    │   ├── event/            # payload Kafka, producer, consumer
    │   └── exception/        # exception riêng của service (nếu common-lib chưa có)
    ├── main/resources/
    │   ├── application.yml   # cấu hình an toàn, không bật profile nào mặc định
    │   ├── application-dev.yml   # đọc infra/.env, bật Swagger
    │   └── db/migration/     # V1__init.sql, V2__...
    └── test/java/com/sqljudge/problem/
        ├── unit/             # test service, mapper, logic thuần
        └── integration/      # Testcontainers: repository, controller, Kafka
```

### Luồng gọi

```
Controller → Service → Repository → MySQL
                  ↘ Client (Feign) → service khác
                  ↘ Event producer → Kafka
```

- Controller: không chứa logic, không gọi Repository trực tiếp, không trả Entity.
- Service: logic nghiệp vụ, transaction, gọi Repository/Client/Producer.
- Repository: chỉ truy vấn. Query phức tạp dùng `@Query` JPQL hoặc native có tham số.
- Không có vòng phụ thuộc giữa các package.

### `judge-worker` - cấu trúc riêng

```
judge-worker/src/main/java/com/sqljudge/judge/
├── consumer/      # nhận job từ Kafka
├── sandbox/       # SandboxPool, PostgresSandbox, MysqlSandbox, SqliteSandbox
├── guard/         # StatementGuard (JSqlParser), giới hạn tài nguyên
├── compare/       # ResultNormalizer, ResultComparator → Verdict
└── publisher/     # gửi kết quả về Kafka
```

Logic so khớp (`compare/`) phải là code thuần, không phụ thuộc DB, để unit test đầy đủ.

### `common-lib`

Chỉ chứa thứ thật sự dùng chung. Package `com.sqljudge.common`:

| Package | Nội dung |
|---|---|
| `error` | `ApiError`, `ErrorCode`, `FieldViolation` |
| `exception` | `AppException` và các lớp con 404, 409, 422, 403, 429, 503 |
| `web` | `GlobalExceptionHandler`, entry point 401 và handler 403 trả `ApiError` |
| `persistence` | `BaseEntity` (id UUIDv7 `BINARY(16)` qua `@UuidV7Id`, cột audit), `UuidV7` |
| `security` | `CurrentUser`, `Role`, `SecurityUtils` |
| `config` | `CommonAutoConfiguration`: tự đăng ký các bean trên, `Clock` UTC và JPA auditing |

Service chỉ cần thêm dependency `common-lib`, không khai báo lại các bean này. Không đưa entity hay logic nghiệp vụ của service vào đây.

---

## Frontend

```
frontend/src/
├── app/
│   ├── (marketing)/          # landing, giới thiệu - SSR/SSG
│   ├── (auth)/login  (auth)/register  onboarding/
│   ├── (app)/                # khung có Top bar: dashboard, problems, learn,
│   │                         # contests, community, leaderboard, u/[username], settings, ai
│   ├── (workspace)/          # toàn màn hình: problems/[slug], contests/[slug]/problems/[letter]
│   ├── (admin)/admin/        # khung có sidebar admin
│   ├── studio/               # Problem Studio, Contest Builder
│   ├── layout.tsx  not-found.tsx  error.tsx
├── components/
│   ├── ui/                   # shadcn/ui - chỉnh bằng token, không sửa logic
│   └── shared/               # VerdictBadge, DifficultyLabel, SqlEditor, ResultTable,
│                             # SchemaTree, TopBar, AdminSidebar, EmptyState…
├── features/<feature>/       # problems, workspace, contest, community, learning, admin, ai…
│   ├── components/
│   ├── hooks/                # useXxxQuery, useXxxMutation (TanStack Query)
│   ├── api.ts                # gọi api-client
│   ├── schemas.ts            # zod cho form
│   └── types.ts
├── lib/                      # api-client.ts, query-client.ts, current-user.ts, format.ts, utils.ts
├── mocks/                    # MSW: handler và fixture cho API giả khi dev và khi test
├── test/                     # tiện ích test dùng chung (render có provider…)
├── stores/                   # Zustand - chưa có, chỉ thêm khi thật cần
└── styles/globals.css        # @theme với token theo design-system.md
```

### Quy tắc import

- `app/` được import `features/`, `components/`, `lib/`.
- `features/A` không import `features/B`. Thứ dùng chung chuyển lên `components/shared` hoặc `lib`.
- `components/ui` và `components/shared` không import `features/`.
- Dùng alias `@/`. Không dùng đường dẫn tương đối quá 1 cấp (`../../`).

### Chỗ đặt một file mới

| Thứ cần thêm | Đặt ở |
|---|---|
| Component dùng ở ≥ 2 feature | `components/shared/` |
| Component chỉ một màn hình | `features/<feature>/components/` |
| Gọi API | `features/<feature>/api.ts` + hook trong `hooks/` |
| Trang mới | `app/<group>/.../page.tsx`, giữ mỏng, ghép component |
| Hằng số, kiểu dùng chung | `lib/` |

---

## Đặt tên file

| Loại | Quy ước | Ví dụ |
|---|---|---|
| Class Java | PascalCase | `SubmissionService.java` |
| Migration | `V<n>__<mo_ta_snake_case>.sql` | `V3__create_testcases_table.sql` |
| Component React | PascalCase | `VerdictBadge.tsx` |
| Hook, util TS | camelCase | `useSubmitSolution.ts`, `formatDuration.ts` |
| Route Next.js | kebab-case | `app/(app)/leaderboard/page.tsx` |
| Test | `<Ten>Test.java`, `<ten>.test.ts(x)`, `<luong>.spec.ts` (E2E) | |

## File môi trường

- `.env.example` (commit) liệt kê mọi biến cần có, giá trị giả.
- `.env`, `.env.local`, `application-secret.yml`, `application-prod.yml`: không commit, Claude không được đọc.
