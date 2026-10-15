---
description: Tìm nguyên nhân gốc của lỗi một cách có hệ thống (test đỏ, build hỏng, lỗi runtime, verdict sai, bài nộp kẹt)
argument-hint: <mô tả lỗi, thông báo lỗi, traceId hoặc id bài nộp>
---

# /diagnose - Tìm nguyên nhân gốc

Lỗi: **$ARGUMENTS**

## Quy tắc dừng dây chuyền

Khi gặp lỗi: **dừng thêm tính năng**. Giữ nguyên bằng chứng, không đoán mò sửa liên tục.

## 6 bước

### 1. Tái hiện
- Có lệnh/test tái hiện được ổn định không? Ghi lại chính xác bước và dữ liệu.
- Lỗi chập chờn → nghi: thời gian, thứ tự message Kafka, chạy song song, dữ liệu dùng chung giữa test.

### 2. Khoanh vùng
- Lỗi ở tầng nào: frontend → gateway → service → Kafka → judge-worker → sandbox?
- Theo `traceId` trong Loki/Grafana để đi xuyên các service.
- Mới hỏng sau một commit? `git bisect start; git bisect bad; git bisect good <commit>`.

### 3. Thu nhỏ
Rút về trường hợp nhỏ nhất còn lỗi: một truy vấn, một testcase, một request.

### 4. Sửa tại gốc
Hỏi "tại sao" cho đến khi gặp nguyên nhân thật. Không chặn triệu chứng (thêm `try/catch` rỗng, tăng timeout cho qua).

### 5. Chặn tái phát
Viết test tái hiện (Prove-It) - đỏ trước, xanh sau khi sửa.

### 6. Xác minh đầu-cuối
Chạy test của service, build, và kiểm tra lại đúng kịch bản ban đầu.

## Cây phân loại theo triệu chứng ở SQL Judge

| Triệu chứng | Kiểm tra trước |
|---|---|
| Bài nộp kẹt "Đang chờ" | Kafka consumer lag của judge-worker; worker có sống không; topic đúng tên không; bài có bị đẩy vào `.dlt` không |
| Verdict sai (AC thành WA) | Chuẩn hoá giá trị trong `ResultComparator` (NULL, số thực, ngày giờ, khoảng trắng), tuỳ chọn so thứ tự của bài, phiên bản dataset |
| Đúng trên PostgreSQL, sai trên MySQL | Khác biệt dialect: collation so sánh chuỗi, kiểu số, thứ tự mặc định, `GROUP BY` lỏng của MySQL |
| `SE` liên tục | Pool sandbox cạn, container sandbox chết, quyền role, dọn schema tạm có chạy không |
| TLE bất thường | Dataset phiên bản mới lớn hơn, thiếu index trong dataset, worker quá tải CPU |
| SSE không nhận verdict | Gateway có buffer response không, header `Content-Type: text/event-stream`, timeout proxy |
| Bảng xếp hạng lệch | Đóng băng, thứ tự event, tính phạt (phút + 20 × lần sai), idempotency |
| 401/403 bất ngờ | Token hết hạn, refresh xoay vòng, đồng hồ lệch, `@PreAuthorize` sai role |
| Lỗi Flyway khi khởi động | Ai đó sửa migration cũ (checksum lệch) - không sửa lại, tạo migration mới |
| Hydration error Next.js | Dùng `Date.now()`/`localStorage` khi render ở server; component cần `'use client'` hoặc `dynamic(..., { ssr: false })` |

## Công cụ

```bash
docker compose -f infra/docker-compose.dev.yml ps
docker compose -f infra/docker-compose.dev.yml logs -f mysql redis mailpit
cd backend && ./mvnw -pl <service> -am test -Dtest=<TenTest> -Dsurefire.failIfNoSpecifiedTests=false
cd backend && ./mvnw clean verify            # lỗi "Unresolved compilation problem": IDE đã ghi đè target/
```

Khi compose dev đã có Kafka và judge-worker: xem lag bằng `kafka-consumer-groups --describe --group judge-worker` trong container Kafka.

Log tạm để gỡ lỗi phải xoá trước khi commit.

## Tránh các lời tự bào chữa

- "Chắc do môi trường" - chưa tái hiện thì chưa kết luận.
- "Tăng timeout là được" - tìm vì sao chậm.
- "Test này hay chập chờn" - test chập chờn là bug.

## Kết quả

Báo cáo ngắn: triệu chứng → nguyên nhân gốc → cách sửa → test đã thêm → cách xác minh.
