# Tech stack - danh sách công nghệ đã chốt

> Chỉ dùng công nghệ trong file này. Muốn thêm thư viện hoặc công nghệ mới: nêu lý do, phương án thay thế, rồi **hỏi người dùng trước**. Nếu được duyệt, ghi một ADR vào `docs/adr/`.

---

## Bảng tổng hợp

| Tầng | Chọn | Không dùng |
|---|---|---|
| Ngôn ngữ backend | Java 21 (LTS) | Kotlin, Node.js cho backend |
| Framework | Spring Boot 3.x | Quarkus, Micronaut |
| Hạ tầng microservice | Spring Cloud Gateway, Eureka, Config Server | Service mesh (chưa cần) |
| Bảo mật | Spring Security, JWT (access + refresh), OAuth2 Client (GitHub, Google) | Session server-side |
| ORM | Spring Data JPA (Hibernate) | MyBatis, Prisma, TypeORM |
| Migration | Flyway | Liquibase, `ddl-auto=update` |
| Mapping | MapStruct | ModelMapper, map tay hàng loạt |
| Validation | Jakarta Bean Validation | Validate thủ công trong service |
| DB ứng dụng | MySQL 8 - mỗi service một database | Dùng chung DB giữa các service |
| Đồng bộ | OpenFeign + Resilience4j (circuit breaker, retry, timeout) | RestTemplate |
| Bất đồng bộ | Apache Kafka | RabbitMQ, BullMQ |
| Cache & bộ đếm | Redis | Memcached, cache trong bộ nhớ khi chạy nhiều instance |
| Realtime | Server-Sent Events (SSE) | Polling dày đặc; WebSocket chỉ khi SSE không đủ |
| Sandbox chấm | Container PostgreSQL 16, MySQL 8, SQLite 3 riêng cho judge-worker | Chạy trên DB ứng dụng |
| Parse SQL | JSqlParser (lọc câu lệnh, chuẩn hoá) | Regex |
| Tài liệu API | springdoc-openapi (OpenAPI 3) | Chỉ có Postman collection |
| Frontend | Next.js 15 App Router, React 19, TypeScript strict | Vite SPA riêng cho admin, JavaScript thuần |
| Style | Tailwind CSS v4 + token từ `docs/DESIGN.md` | CSS-in-JS runtime, MUI |
| Component | shadcn/ui (Radix) | Chakra, Ant Design |
| Editor SQL | Monaco Editor (`@monaco-editor/react`) | CodeMirror (trừ khi có lý do) |
| Server state | TanStack Query | SWR, fetch rải rác trong component |
| Client state | Zustand (chỉ khi thật sự cần chia sẻ) | Redux |
| Form | react-hook-form + zod | Formik |
| Bảng dữ liệu | TanStack Table | Tự viết bảng có sort/filter |
| Biểu đồ | Recharts | Chart.js |
| Font | Be Vietnam Pro (giao diện), JetBrains Mono (code) | Inter, Roboto |
| Test backend | JUnit 5, Mockito, AssertJ, Testcontainers (MySQL, Kafka, sandbox) | H2 thay MySQL trong integration test |
| Test frontend | Vitest + Testing Library, MSW | Jest |
| E2E | Playwright | Cypress |
| Log | Logback JSON (logstash-encoder) → Loki | `System.out.println` |
| Metric | Micrometer → Prometheus → Grafana | - |
| Tracing | Micrometer Tracing + OpenTelemetry | - |
| CI/CD | GitHub Actions, image lên GHCR | Jenkins |
| Triển khai | Docker Compose trên VPS (giai đoạn đầu) | Kubernetes (chưa cần) |
| LLM | Gọi qua `ai-service` duy nhất, không gọi trực tiếp từ frontend | Nhúng API key ở client |

---

## Backend

### Vì sao Spring Boot microservices

- Máy chấm (`judge-worker`) có tải và mô hình bảo mật khác hẳn phần còn lại, nên cần tách riêng để scale và khoá quyền độc lập.
- Contest tạo đỉnh tải ngắn (hàng nghìn bài nộp trong 90 phút). Kafka làm bộ đệm giữa `submission-service` và `judge-worker`, nên nhận bài không bị nghẽn khi chấm chậm.

### Danh sách service và trách nhiệm

| Service | Sở hữu dữ liệu | Ghi chú |
|---|---|---|
| `api-gateway` | - | Định tuyến, xác thực JWT, rate limit, CORS |
| `discovery-server` | - | Eureka |
| `config-server` | - | Cấu hình tập trung, bí mật lấy từ biến môi trường |
| `auth-service` | users, roles, oauth_accounts, refresh_tokens, profiles | Phát JWT |
| `problem-service` | problems, datasets, testcases, tags, problem_reviews | Lưu snapshot dataset cho sandbox |
| `learning-service` | courses, chapters, lessons, quizzes, progress | Ô SQL trong bài giảng gọi chế độ "run" |
| `submission-service` | submissions, submission_results | Nhận bài, phát job, đẩy verdict qua SSE |
| `judge-worker` | Không có DB ứng dụng | Chỉ đọc job từ Kafka, ghi kết quả qua Kafka |
| `contest-service` | contests, registrations, standings, ratings, clarifications | Leaderboard trên Redis sorted set |
| `community-service` | posts, comments, votes, reports | Kiểm duyệt |
| `ai-service` | ai_conversations, ai_quotas, ai_flags | Áp chính sách gợi ý, tắt khi đang thi |
| `notification-service` | notifications | Email, thông báo trong app |

Không tự tạo service mới. Nếu một tính năng không khớp service nào, dừng lại và hỏi.

### Dependency tối thiểu mỗi service

`spring-boot-starter-web`, `-validation`, `-data-jpa`, `-security`, `-actuator`, `mysql-connector-j`, `flyway-core` + `flyway-mysql`, `mapstruct`, `lombok` (chỉ `@Getter/@Setter/@Builder/@RequiredArgsConstructor`, không `@Data` trên entity), `springdoc-openapi-starter-webmvc-ui`, `micrometer-registry-prometheus`, `common-lib` nội bộ.

---

## Frontend

### Vì sao một app Next.js

- Trang công khai (landing, danh sách bài, khoá học, bài viết cộng đồng) cần SSR để SEO.
- Workspace, contest và admin là phần tương tác nặng, chạy dưới dạng Client Component trong cùng app, dùng chung design system và phiên đăng nhập.
- Admin tách bằng route group `(admin)`, không cần codebase riêng. Vai trò được kiểm tra ở client trong khung admin (`AdminShell`, đọc `/api/auth/me`) để không render và không gọi API admin cho người không phải `ADMIN`; đây chỉ là trải nghiệm. Rào chắn thật là `@PreAuthorize` ở từng service. Chưa dùng `middleware.ts` vì access token nằm trong bộ nhớ, middleware không đọc được vai trò; xem lại khi có cookie phiên mà server đọc được.

### Nguyên tắc

- Server Component mặc định. Chỉ thêm `'use client'` cho component có state, event handler hoặc dùng API trình duyệt.
- Monaco Editor luôn import động với `ssr: false`.
- Mọi lời gọi API đi qua `src/lib/api-client.ts`, trỏ tới `api-gateway`. Không gọi thẳng từng service.
- Next.js không chứa business logic. Không dùng Route Handler hay Server Action để thay thế API backend; chỉ dùng chúng để chuyển tiếp cookie hoặc xử lý upload nếu thật cần.

---

## Quy trình thêm dependency

1. Kiểm tra thư viện đã có làm được chưa.
2. Kiểm tra license (MIT, Apache 2.0, BSD), mức bảo trì (commit trong 6 tháng), số lỗ hổng đã biết.
3. Frontend: kiểm tra kích thước bundle; thư viện lớn hơn 30 KB gzip cần lý do.
4. Hỏi người dùng, ghi ADR nếu được duyệt.
