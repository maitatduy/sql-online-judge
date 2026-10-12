# Máy chấm & sandbox - quy tắc bắt buộc

> SQL người dùng gửi lên là **code không tin cậy**. File này có mức ưu tiên ngang `security.md`. Mọi thay đổi trong `judge-worker`, `submission-service`, phần dataset/testcase của `problem-service`, và ô "Chạy thử" trong bài giảng đều phải tuân theo. Vi phạm bất kỳ mục 🔴 nào là lỗi chặn merge.

---

## 1. Luồng chấm

```
Frontend ──POST /api/submissions──▶ submission-service
                                      │  lưu submission (PENDING)
                                      │  publish sqljudge.submission.created
                                      ▼
                                   Kafka ──▶ judge-worker (N instance)
                                               │ 1. kiểm tra câu lệnh (guard)
                                               │ 2. xin sandbox từ pool
                                               │ 3. với từng testcase: nạp dataset → chạy → so khớp
                                               │ 4. dọn sandbox
                                               ▼
                                   Kafka ◀── sqljudge.judge.completed / .progress
                                      │
submission-service cập nhật verdict ──┴──▶ SSE /api/submissions/{id}/events ──▶ Frontend
                                      └──▶ contest-service (nếu là bài thi) cập nhật standings
```

Chế độ **Run** (chạy thử với dataset mẫu, không tính điểm) đi cùng đường nhưng topic `sqljudge.run.requested`, ưu tiên thấp hơn trong contest và không bao giờ dùng testcase ẩn.

---

## 2. Cô lập - 🔴 bắt buộc

- 🔴 Sandbox là các container DB **riêng** (PostgreSQL, MySQL, SQLite) chỉ `judge-worker` kết nối tới. Không dùng chung host/instance với MySQL ứng dụng.
- 🔴 Container sandbox nằm trong network Docker riêng, không có route ra Internet, không thấy network của service ứng dụng.
- 🔴 `judge-worker` không có thông tin đăng nhập vào bất kỳ DB ứng dụng nào. Nó chỉ đọc job từ Kafka và tải snapshot dataset qua đường đọc-only (object storage hoặc API nội bộ của `problem-service`).
- 🔴 Mỗi lần chạy dùng một **schema/database tạm** có tên ngẫu nhiên (`sb_<uuid>`), tạo từ snapshot dataset và **xoá ngay** sau khi chấm, kể cả khi lỗi (`finally`).
- 🔴 Không bao giờ tái sử dụng schema đã chạy SQL người dùng cho lần chạy khác.

## 3. Tài khoản và quyền trong sandbox - 🔴 bắt buộc

- 🔴 SQL người dùng chạy bằng một role riêng có quyền tối thiểu:
  - Bài loại SELECT: chỉ `SELECT` trên schema tạm của lần chạy đó.
  - Bài loại DML/DDL (ví dụ bài về UPDATE, CREATE INDEX): quyền giới hạn trong đúng schema tạm.
- 🔴 Role không có quyền: tạo/xoá database, tạo role, đọc catalog hệ thống ngoài schema của mình, gọi hàm hệ thống nguy hiểm (`pg_read_file`, `COPY … PROGRAM`, `LOAD_FILE`, `INTO OUTFILE`, `pg_sleep` nếu đề không cho), cài extension.
- PostgreSQL: `REVOKE ALL ON SCHEMA public FROM PUBLIC`, `search_path` cố định về schema tạm, `default_transaction_read_only=on` với bài SELECT.
- MySQL: `secure_file_priv` rỗng/NULL, `local_infile=OFF`, user chỉ có quyền trên database tạm.
- SQLite: mở file tạm bằng URI `mode=ro` với bài SELECT, tắt `load_extension`, không cho `ATTACH`.

## 4. Giới hạn tài nguyên - 🔴 bắt buộc

| Giới hạn | Mặc định | Cách áp |
|---|---|---|
| Thời gian mỗi testcase | 2000 ms (bài có thể đặt tối đa 6000 ms) | PG `statement_timeout`, MySQL `max_execution_time`, phía Java `Statement.setQueryTimeout` + timeout cứng của worker |
| Số dòng trả về | 10 000 | `setMaxRows`; vượt thì dừng đọc, verdict theo luật của đề |
| Kích thước kết quả | 5 MB | Đếm byte khi đọc ResultSet |
| Số câu lệnh mỗi bài nộp | 1 (bài SELECT), tối đa 20 (bài DML) | StatementGuard |
| Độ dài mã nguồn | 64 KB | Validate ở `submission-service` |
| CPU / RAM container | Theo `infra/sandbox` | Docker `--cpus`, `--memory` |

Hết thời gian → `TLE`. Worker treo quá timeout cứng → huỷ connection, đánh dấu `SE`, tự đưa lại hàng đợi tối đa 2 lần.

## 5. StatementGuard (JSqlParser)

- Parse trước khi chạy để trả verdict `FS` sớm và dễ hiểu ("Bài này chỉ cho phép SELECT").
- Danh sách lệnh cho phép lấy từ cấu hình bài (`allowed_statements`), mặc định chỉ `SELECT`/`WITH`.
- Parse lỗi theo dialect → trả `RE` với thông báo lỗi của parser hoặc của DB, không chặn nhầm.
- ⚠️ Guard **không phải** rào chắn bảo mật. Nếu guard bỏ sót, quyền DB và cô lập container vẫn phải chặn được. Không bao giờ nới quyền DB với lý do "đã có guard".

## 6. So khớp kết quả → verdict

`ResultComparator` là code thuần, có unit test cho từng nhánh:

1. Lỗi khi chạy → `RE` (lưu thông báo lỗi đã lọc, xem mục 7).
2. Số cột khác → `WA`.
3. Tên cột: chỉ so khi đề bật `compare_column_names`.
4. Chuẩn hoá giá trị: `NULL` ≠ chuỗi `"NULL"`; số thực so với sai số `1e-6` (cấu hình được); ngày giờ chuẩn hoá về ISO-8601 UTC; chuỗi giữ nguyên (không trim, không đổi hoa thường) trừ khi đề cấu hình.
5. Đề yêu cầu thứ tự: so tuần tự. Multiset khớp mà thứ tự sai → `OM`, không khớp → `WA`.
6. Đề không yêu cầu thứ tự: so multiset (đếm số lần xuất hiện, không dùng set - dòng trùng lặp vẫn tính).
7. Mọi testcase đúng → `AC`. Dừng ở testcase sai đầu tiên, trừ khi contest dùng điểm từng phần.

Ghi lại: verdict, testcase sai đầu tiên, thời gian chạy lớn nhất, số dòng, phiên bản engine.

## 7. Bảo vệ dữ liệu testcase - 🔴 bắt buộc

- 🔴 Testcase ẩn: response chỉ trả verdict, số thứ tự testcase, thời gian. Không trả dữ liệu, không trả diff, không trả số dòng mong đợi.
- 🔴 Testcase công khai: được trả diff kết quả.
- 🔴 Trong contest: không trả diff kể cả với testcase công khai, trừ khi đề cho phép.
- 🔴 Thông báo lỗi DB trả cho người dùng phải được lọc: bỏ tên schema tạm, đường dẫn file, host, phiên bản nội bộ.
- 🔴 `ai-service` không bao giờ nhận dataset của testcase ẩn hay đáp án chuẩn khi người dùng chưa AC.
- 🔴 Log của worker không ghi nội dung dataset ẩn, chỉ ghi id.

## 8. Dataset và đáp án chuẩn

- Dataset lưu dưới dạng script SQL đã chuẩn hoá theo từng dialect + checksum. Snapshot là bất biến: sửa dataset tạo phiên bản mới, bài nộp cũ vẫn tham chiếu phiên bản cũ.
- Đáp án chuẩn phải chạy ra cùng kết quả trên mọi dialect bài hỗ trợ; Problem Studio chặn xuất bản nếu lệch.
- Kết quả mong đợi tính sẵn khi xuất bản bài (chạy đáp án chuẩn trong sandbox), lưu kèm checksum. Worker so với kết quả này, không chạy lại đáp án chuẩn mỗi lần chấm.

## 9. Độ tin cậy

- Job có `submissionId` làm khoá idempotency: xử lý lại cùng job không tạo kết quả trùng.
- Consumer commit offset sau khi đã publish kết quả.
- Worker chết giữa chừng: job được Kafka giao lại; sandbox mồ côi bị dọn bởi tác vụ định kỳ (xoá schema `sb_*` cũ hơn 10 phút).
- Chấm lại hàng loạt (rejudge) đi qua topic riêng `sqljudge.submission.rejudge-requested` với độ ưu tiên thấp hơn bài nộp mới.

## 10. Kiểm thử bắt buộc cho thay đổi máy chấm

- Unit test `ResultComparator`: mỗi verdict, NULL, số thực, dòng trùng, thứ tự, bảng rỗng.
- Unit test `StatementGuard`: lệnh cho phép / bị cấm theo từng dialect, nhiều câu lệnh, comment lồng lệnh cấm.
- Integration test (Testcontainers) với **bộ truy vấn tấn công** tối thiểu, tất cả phải bị chặn hoặc vô hại:
  - `DROP`/`TRUNCATE` bảng dataset, `DROP DATABASE`, tạo role.
  - Đọc file hệ thống (`pg_read_file`, `LOAD_FILE`, `COPY ... FROM PROGRAM`, `INTO OUTFILE`).
  - Truy cập schema của lần chạy khác hoặc `information_schema` ngoài phạm vi.
  - Vòng lặp vô hạn / CTE đệ quy không điểm dừng / cross join khổng lồ → `TLE`, không làm treo worker.
  - Trả về hàng triệu dòng → bị cắt theo giới hạn.
  - `pg_sleep(60)`, `SELECT BENCHMARK(...)`, `SLEEP()`.
  - Nhiều câu lệnh nối bằng `;` trong bài chỉ cho một câu.
