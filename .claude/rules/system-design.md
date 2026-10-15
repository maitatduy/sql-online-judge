# Thiết kế hệ thống

## Bối cảnh tải cần thiết kế cho

- Tải thường ngày thấp và đều; **đỉnh tải là contest**: hàng nghìn người nộp trong 90 phút, dồn mạnh ở 10 phút đầu và 15 phút cuối.
- Thao tác tốn tài nguyên nhất là **chấm bài** (khởi tạo dataset + chạy SQL), không phải đọc trang.
- Dữ liệu đọc nhiều, ít đổi: đề bài, khoá học, tag, bài viết đã đăng.
- Dữ liệu ghi liên tục: bài nộp, bảng xếp hạng contest, bộ đếm AI.

Ước lượng sơ bộ (điều chỉnh khi có số liệu thật): contest 2.000 người, mỗi người ~15 lần nộp → 30.000 bài nộp / 90 phút ≈ 6 bài/giây trung bình, đỉnh ~30 bài/giây. Mỗi bài 8 testcase × ~50 ms → cần khoảng 12 luồng chấm song song ở đỉnh, dư gấp đôi để an toàn.

## Nguyên tắc

1. **Tách nhận bài khỏi chấm bài.** `submission-service` chỉ lưu và trả 202; Kafka làm bộ đệm; `judge-worker` scale ngang theo độ dài hàng đợi.
2. **Thiết kế cho lỗi.** Mọi lời gọi mạng có timeout; Feign có circuit breaker; consumer idempotent; worker chết thì job được giao lại.
3. **Một nguồn sự thật cho mỗi dữ liệu.** Service sở hữu dữ liệu là nơi duy nhất ghi. Service khác giữ bản sao chỉ-đọc cập nhật bằng event nếu cần.
4. **Đơn giản trước.** Docker Compose một VPS, một broker Kafka, một Redis cho giai đoạn đầu. Chỉ tách cụm khi số liệu monitoring cho thấy cần.

## Giao tiếp giữa service

| Cần | Dùng |
|---|---|
| Dữ liệu ngay để trả response (lấy tên người dùng cho bảng xếp hạng) | OpenFeign + timeout + fallback; cân nhắc cache Redis |
| Báo cho service khác một việc đã xảy ra (bài đã chấm, contest kết thúc) | Kafka event |
| Việc nặng chạy nền (chấm lại hàng loạt, tính rating sau contest, gửi email) | Kafka + consumer riêng, không làm trong request |

- Event phát qua **transactional outbox** (xem `database.md`) để không mất event khi DB commit nhưng Kafka lỗi.
- Không tạo chuỗi gọi đồng bộ dài hơn 2 bước (A → B → C). Nếu cần, xem lại ranh giới service hoặc dùng event.

## Idempotency

- Consumer lưu `eventId` đã xử lý (bảng `processed_events` hoặc Redis set có TTL) và bỏ qua bản trùng.
- `POST` nộp bài và đăng ký contest nhận `Idempotency-Key`.
- Tính rating sau contest chạy một lần, đánh dấu `rating_applied_at` trên contest.

## Bảng xếp hạng contest

- Redis sorted set, điểm mã hoá `solved * 10^7 - penalty` để một lần `ZREVRANGE` ra đúng thứ hạng.
- Chi tiết từng ô (số lần sai, phút AC) lưu trong hash `sqljudge:contest:cells:{contestId}`.
- Đóng băng: từ mốc đóng băng, bài nộp mới vẫn được chấm và lưu, nhưng standings công khai không cập nhật; người thi vẫn thấy kết quả của chính mình.
- MySQL là nguồn sự thật: khi contest kết thúc, ghi bảng xếp hạng cuối vào `contest_standings` và tính rating.
- Đẩy cập nhật qua SSE, gộp theo nhịp 2 giây để tránh bắn quá nhiều sự kiện.

## Cache

| Dữ liệu | Chiến lược |
|---|---|
| Đề bài, khoá học, tag | Cache-aside, TTL 10–60 phút, xoá khi có event cập nhật |
| Trang công khai Next.js | ISR, `revalidate` 300 giây, revalidate theo tag khi bài cập nhật |
| Hồ sơ người dùng cho bảng xếp hạng | Cache-aside 5 phút |
| Không cache | Verdict, quota AI (đã là bộ đếm Redis), dữ liệu admin |

Tránh cache stampede khi contest bắt đầu: làm nóng cache đề bài contest trước giờ bắt đầu 1 phút.

## Độ tin cậy

- Circuit breaker: mở khi ≥ 50% lỗi trong 20 lời gọi gần nhất, nửa mở sau 30 giây.
- Retry: tối đa 3 lần, backoff mũ có jitter, chỉ cho lỗi tạm thời và thao tác idempotent.
- Health check: `/actuator/health/liveness` và `/readiness`; readiness kiểm tra DB, Kafka, Redis (và sandbox pool với judge-worker).
- Graceful shutdown: worker hoàn tất job đang chấm (tối đa 30 giây) trước khi dừng.

## Phân trang

- Offset (`page`, `size`) cho danh sách admin và danh sách bài có lọc.
- Cursor cho feed thời gian thực (bài nộp gần nhất, bình luận, thông báo).

## Ghi quyết định

Quyết định kiến trúc mới (thêm service, đổi cách lưu trữ, đổi giao thức) trình bày theo mẫu trong agent `systems-architect` để người dùng duyệt, rồi ghi tóm tắt vào mô tả PR. Không tạo thư mục tài liệu trong repo.
