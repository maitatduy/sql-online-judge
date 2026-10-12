# Giám sát - log, metric, trace, cảnh báo

Giai đoạn đầu chạy gọn trên một VPS: Prometheus + Grafana + Loki trong `infra/monitoring/`. Không thêm công cụ khác khi chưa cần.

## Log

- Logback xuất **JSON một dòng** (logstash-logback-encoder), đẩy về Loki.
- Trường bắt buộc: `timestamp`, `level`, `service`, `traceId`, `spanId`, `message`; thêm `userId`, `submissionId`, `contestId` khi có (qua MDC).
- Mức log:

| Mức | Dùng khi |
|---|---|
| ERROR | Lỗi cần người xử lý: worker không tạo được sandbox, consumer đẩy vào DLT |
| WARN | Bất thường nhưng tự phục hồi: retry Feign, circuit breaker mở, verdict `SE` |
| INFO | Sự kiện nghiệp vụ: bài nộp đã chấm, contest bắt đầu/kết thúc, bài được duyệt |
| DEBUG | Chi tiết để gỡ lỗi; tắt ở production |

- Đặt `message` dạng sự kiện snake_case dễ lọc: `submission_judged`, `sandbox_create_failed`.
- 🔴 Không log: mật khẩu, token, cookie, nội dung SQL người dùng ở mức INFO trở lên (chỉ ghi độ dài + hash), dataset ẩn, prompt/response AI đầy đủ (chỉ ghi id hội thoại).

## Metric (Micrometer → Prometheus)

Mọi service bật `/actuator/prometheus` (chỉ mở trong mạng nội bộ). Ngoài metric mặc định của Spring (HTTP, JVM, HikariCP, Kafka), thêm:

| Metric | Loại | Nhãn |
|---|---|---|
| `sqljudge_judge_duration_seconds` | histogram | `dialect`, `verdict` |
| `sqljudge_judge_queue_size` | gauge | `topic` |
| `sqljudge_judge_verdict_total` | counter | `dialect`, `verdict` |
| `sqljudge_judge_sandbox_errors_total` | counter | `dialect`, `reason` |
| `sqljudge_submission_accepted_total` | counter | `source` (practice/contest) |
| `sqljudge_contest_active_participants` | gauge | `contest_id` (chỉ contest đang chạy) |
| `sqljudge_ai_messages_total` | counter | `mode`, `plan` |
| `sqljudge_ai_tokens_total` | counter | `direction` (in/out) |

- Không đưa giá trị nhiều biến thể (userId, submissionId) vào nhãn.
- Đơn vị luôn ở hậu tố tên (`_seconds`, `_bytes`, `_total`).

## Trace

Micrometer Tracing + OpenTelemetry; `traceId` truyền qua gateway → service → Kafka header → judge-worker, để theo một bài nộp từ lúc nhấn Submit tới lúc có verdict. Trả `traceId` trong `ApiError` để người dùng gửi kèm khi báo lỗi.

## Dashboard Grafana

| Dashboard | Nội dung |
|---|---|
| SQL Judge - Tổng quan | Người dùng hoạt động, bài nộp/phút, tỉ lệ lỗi HTTP, p95 độ trễ API |
| Máy chấm | Hàng đợi, thời gian chấm p50/p95/p99 theo dialect, phân bố verdict, lỗi sandbox, CPU/RAM worker |
| Contest đang chạy | Người thi, bài nộp/phút, độ trễ từ nộp đến có verdict |
| AI | Tin nhắn, token, chi phí ước tính, tỉ lệ bị chặn theo chính sách |
| Hạ tầng | MySQL, Kafka lag, Redis, dung lượng ổ đĩa |

Mỗi service theo phương pháp RED (Rate, Errors, Duration); hạ tầng theo USE (Utilization, Saturation, Errors).

## Cảnh báo

| Mức | Ví dụ | Hành động |
|---|---|---|
| P1 - khẩn | Hàng đợi chấm > 500 trong 5 phút khi có contest; tất cả worker down; tỉ lệ 5xx > 5% | Báo ngay (Telegram/email trực) |
| P2 - cao | p95 thời gian chấm > 5 giây trong 10 phút; một worker down; Kafka consumer lag tăng liên tục | Xử lý trong giờ |
| P3 - theo dõi | Chi phí AI vượt 80% ngân sách ngày; ổ đĩa > 80% | Xem trong ngày |

Tên cảnh báo: `<Service><VấnĐề>` dạng PascalCase, ví dụ `JudgeQueueBacklogHigh`, `JudgeWorkerDown`. Mỗi cảnh báo có link tới dashboard và một dòng hướng dẫn xử lý.
