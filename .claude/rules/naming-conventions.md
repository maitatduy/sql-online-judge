# Quy ước đặt tên

Tên trong code, DB, topic, biến môi trường viết **tiếng Anh**. Chữ hiển thị cho người dùng viết **tiếng Việt**.

## Java

| Thứ | Quy ước | Ví dụ |
|---|---|---|
| Class, record, enum | PascalCase | `SubmissionService`, `JudgeResult`, `Verdict` |
| Method, biến | camelCase, động từ cho method | `submitSolution()`, `firstFailedCase` |
| Hằng số | UPPER_SNAKE | `MAX_RESULT_ROWS` |
| Package | chữ thường | `com.sqljudge.contest.service` |
| DTO request | `<Hành động><Thực thể>Request` | `CreateProblemRequest`, `SubmitSolutionRequest` |
| DTO response | `<Thực thể><Mức>Response` | `ProblemSummaryResponse`, `ProblemDetailResponse` |
| Mapper | `<Thực thể>Mapper` | `ProblemMapper` |
| Feign client | `<Service>Client` | `AuthServiceClient` |
| Event payload | `<Thực thể><Quá khứ>Event` | `SubmissionCreatedEvent`, `JudgeCompletedEvent` |

## TypeScript / React

| Thứ | Quy ước | Ví dụ |
|---|---|---|
| Component | PascalCase | `VerdictBadge`, `ProblemTable` |
| Hook | `use` + camelCase | `useSubmission`, `useContestStandings` |
| Query key | mảng, phần tử đầu là tên tài nguyên | `['problems', { topic, page }]`, `['submission', id]` |
| Kiểu, interface | PascalCase, không tiền tố `I` | `Problem`, `SubmissionStatus` |
| Biến boolean | `is/has/can/should` | `isJudging`, `canUseAi` |
| Handler | `handle` + sự kiện | `handleSubmit`, `handleRunQuery` |

## Database

Xem bảng đặt tên trong `database.md`. Tên database: `sqljudge_<service>` (`sqljudge_auth`, `sqljudge_problem`).

## Kafka

- Topic: `sqljudge.<domain>.<event-quá-khứ>` - chữ thường, phân cách bằng dấu chấm, sự kiện dùng gạch ngang.
- Dead letter: thêm hậu tố `.dlt`.

| Topic | Producer → Consumer |
|---|---|
| `sqljudge.submission.created` | submission → judge-worker |
| `sqljudge.run.requested` | submission → judge-worker (chạy thử) |
| `sqljudge.submission.rejudge-requested` | submission → judge-worker |
| `sqljudge.judge.progress` | judge-worker → submission (đang chấm testcase n/m) |
| `sqljudge.judge.completed` | judge-worker → submission |
| `sqljudge.submission.judged` | submission → contest, auth (thống kê), notification |
| `sqljudge.problem.published` | problem → learning, notification |
| `sqljudge.contest.started` / `.finished` | contest → ai (khoá/mở AI), notification |
| `sqljudge.contest.rating-updated` | contest → auth (cập nhật rating hồ sơ) |
| `sqljudge.user.registered` | auth → notification |
| `sqljudge.community.reported` | community → notification (admin) |

Payload event: JSON, có `eventId` (UUID), `occurredAt`, `version`, và dữ liệu nghiệp vụ tối thiểu. Key của message là id thực thể (giữ thứ tự theo thực thể).

## Redis

Khoá: `sqljudge:<service>:<thực-thể>:<id>[:<thuộc-tính>]` - chữ thường, phân cách bằng `:`.

| Mục đích | Khoá | TTL |
|---|---|---|
| Cache chi tiết bài | `sqljudge:problem:detail:{slug}` | 10 phút, xoá khi bài cập nhật |
| Danh sách tag | `sqljudge:problem:tags` | 1 giờ |
| Bảng xếp hạng contest | `sqljudge:contest:standings:{contestId}` (sorted set) | đến khi contest kết thúc + 7 ngày |
| Rate limit nộp bài | `sqljudge:gateway:ratelimit:submit:{userId}` | 60 giây |
| Hạn mức AI | `sqljudge:ai:quota:{userId}:{yyyyMMdd}` | 26 giờ |
| Khoá chống chấm trùng | `sqljudge:judge:lock:{submissionId}` | 2 phút |
| Refresh token bị thu hồi | `sqljudge:auth:revoked:{jti}` | bằng thời hạn token |

Mọi khoá cache phải có TTL. Không lưu dữ liệu chỉ tồn tại trong Redis mà không có nguồn gốc ở MySQL (trừ bộ đếm tạm).

## Biến môi trường

- UPPER_SNAKE, tiền tố theo nhóm: `MYSQL_`, `KAFKA_`, `REDIS_`, `JWT_`, `OAUTH_GITHUB_`, `LLM_`, `SANDBOX_`.
- Ví dụ: `MYSQL_HOST`, `KAFKA_BOOTSTRAP_SERVERS`, `JWT_ACCESS_TTL_MINUTES`, `SANDBOX_PG_URL`, `LLM_API_KEY`.
- Frontend: chỉ biến không bí mật mới có tiền tố `NEXT_PUBLIC_` (`NEXT_PUBLIC_API_URL`). Không bao giờ đặt khoá bí mật vào biến `NEXT_PUBLIC_`.
- Mọi biến mới phải thêm tên vào `.env.example`, để trống giá trị. Sau khi sao chép thành `.env` phải điền đủ mọi biến, kể cả cổng: biến rỗng không dùng được giá trị mặc định của Spring.

## Metric Prometheus

`sqljudge_<subsystem>_<tên>_<đơn-vị>` - ví dụ `sqljudge_judge_duration_seconds`, `sqljudge_judge_queue_size`, `sqljudge_ai_messages_total`. Chi tiết: `monitoring.md`.

## Git

Nhánh và commit: xem `git-workflow.md`.
