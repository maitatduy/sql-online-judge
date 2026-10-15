# Checklist hiệu năng

## Mục tiêu

| Chỉ số | Mục tiêu |
|---|---|
| LCP trang công khai | < 2,5 s (4G) |
| INP | < 200 ms |
| CLS | < 0,1 |
| JS tải ban đầu (trang công khai) | < 170 KB gzip |
| API đọc thông thường p95 | < 200 ms |
| Nộp bài → nhận 202 p95 | < 150 ms |
| Nộp bài → có verdict p95 (bình thường) | < 3 s |
| Nộp bài → có verdict p95 (đỉnh contest) | < 8 s |
| Cập nhật bảng xếp hạng tới client | ≤ 2 s |

## Frontend

- [ ] Trang công khai là Server Component, dùng ISR; dữ liệu đầu trang có sẵn trong HTML.
- [ ] Monaco import động (`ssr: false`) chỉ ở workspace/bài giảng; không lọt vào bundle trang khác.
- [ ] Font qua `next/font`, chỉ subset `latin` + `vietnamese`, `display: swap`.
- [ ] Ảnh qua `next/image`, có `width/height` hoặc `sizes`.
- [ ] Bảng dài (bảng xếp hạng, kết quả truy vấn > 200 dòng) dùng ảo hoá dòng.
- [ ] Workspace: gõ trong editor không làm render lại panel kết quả và panel đề (tách state, `memo`).
- [ ] SSE cập nhật bảng xếp hạng được gộp theo nhịp, không render lại cả bảng mỗi sự kiện.
- [ ] Không import cả thư viện cho một hàm (`date-fns/format` thay vì toàn bộ).
- [ ] Phân tích bundle: `ANALYZE=true npm run build` (cần cài `@next/bundle-analyzer` trước; không chạy `npm run build` khi `npm run dev` đang chạy vì build ghi đè `.next`).

## Backend

- [ ] Danh sách dùng projection + phân trang; không tải cả entity.
- [ ] Không N+1 (đếm câu lệnh trong test bằng Hibernate statistics).
- [ ] Index cho mọi cột lọc/sắp xếp mới; kiểm tra bằng `EXPLAIN`.
- [ ] Thống kê (số người giải, tỉ lệ AC) cập nhật dồn qua event, không `COUNT(*)` lúc đọc.
- [ ] Cache-aside cho đề bài, khoá học, tag; làm nóng cache đề contest trước giờ bắt đầu.
- [ ] HikariCP: pool vừa đủ (mặc định 10/instance), theo dõi thời gian chờ connection.
- [ ] Việc nặng (chấm lại, tính rating, gửi email hàng loạt) chạy nền qua Kafka.
- [ ] Feign có timeout; không chuỗi gọi đồng bộ dài.

## Máy chấm

- [ ] Pool sandbox khởi tạo sẵn (warm), không tạo container mới cho mỗi bài nộp.
- [ ] Nạp dataset bằng script đã chuẩn hoá, có index cần thiết; tạo schema từ template khi DB hỗ trợ.
- [ ] Dừng ở testcase sai đầu tiên (trừ contest điểm từng phần).
- [ ] Số luồng chấm mỗi worker phù hợp số CPU; scale số worker theo `sqljudge_judge_queue_size`.
- [ ] Chạy thử (Run) có độ ưu tiên thấp hơn bài nộp trong giờ contest.

## Đo

```bash
# Frontend
npx lighthouse http://localhost:3000/problems --view
ANALYZE=true npm run build

# Backend
k6 run infra/load/contest-submit.js          # mô phỏng 2.000 thí sinh - kịch bản chưa có, viết khi làm contest
EXPLAIN ANALYZE SELECT …;                     # trong MySQL
```

Theo dõi trên dashboard Grafana "Máy chấm" và "Contest đang chạy" trong lúc chạy kiểm thử tải.
