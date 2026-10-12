---
name: incremental-implementation
description: Thi công theo lát cắt dọc nhỏ, mỗi bước build được và test xanh. Dùng khi bắt đầu một tính năng nhiều phần (backend + frontend + máy chấm), khi chia task, hoặc khi thay đổi đang phình quá lớn.
---

# Thi công tăng dần theo lát cắt dọc

> "Thứ đơn giản nhất có thể chạy được."

## Chu trình một bước

```
Chọn hành vi nhỏ nhất → Test đỏ → Code tối thiểu → Test xanh → Làm gọn → Commit → Lặp lại
```

## Lát cắt dọc, không phải lát cắt ngang

**Dọc (đúng):** mỗi bước đi xuyên mọi tầng cho **một** hành vi, demo được.

```
Bước 1: Nộp bài PostgreSQL, 1 testcase, chỉ AC/WA  → migration + API + worker + nút Submit + test
Bước 2: Nhiều testcase, dừng ở testcase sai đầu tiên
Bước 3: Verdict OM khi đề yêu cầu thứ tự
Bước 4: Hiển thị tiến độ "Đang chấm 3/8" qua SSE
Bước 5: Thêm MySQL
```

**Ngang (sai):** làm hết toàn bộ bảng → rồi toàn bộ API → rồi toàn bộ giao diện. Đến cuối mới biết các phần không khớp nhau.

## Chiến lược chọn lát cắt

| Chiến lược | Khi nào | Ví dụ |
|---|---|---|
| Đường chính trước | Mặc định | Nộp bài đúng → AC trước, rồi mới các verdict lỗi |
| Rủi ro trước | Có phần chưa chắc làm được | Dựng sandbox PostgreSQL cô lập trước khi làm giao diện workspace |
| Hợp đồng trước | Backend và frontend làm song song | Chốt DTO + MSW mock, frontend làm theo mock |

Với SQL Judge: **máy chấm là rủi ro lớn nhất** - dựng lát cắt chấm bài tối thiểu sớm nhất.

## Quy tắc

- **~100 dòng**: viết quá ~100 dòng mà chưa chạy test → dừng lại, chạy test.
- **Chỉ chạm thứ cần**: không đổi tên, định dạng, "dọn dẹp" ngoài phạm vi.
- **Luôn build được**: không để `main` (hay nhánh) ở trạng thái hỏng giữa hai bước.
- **Feature flag** cho phần chưa xong cần merge (`sqljudge.feature.mysql-dialect=false`).
- **Mặc định an toàn**: tính năng mới tắt, quyền mới hẹp, giới hạn mới chặt.
- **Dễ lùi lại**: mỗi commit đứng được một mình; migration chỉ cộng thêm.

## Commit

```
✅ feat: compare single-testcase SELECT result for PostgreSQL
✅ feat: stop at first failed testcase
✅ feat: show judging progress in workspace

❌ feat: judge system + workspace + contest scoring (2.000 dòng)
```

## Dấu hiệu sai hướng

- Nhiều giờ không chạy test.
- Thay đổi chạm > 10 file không liên quan nhau.
- "Để làm xong hết rồi test một lượt."
- Phải sửa lại thứ vừa làm ở bước trước vì nó không khớp.

## Khi bị kẹt

1. Thu nhỏ bước hơn nữa.
2. Viết test cho phần nhỏ nhất còn chưa chắc.
3. Sau 2 lần thử không được: dừng, trình bày đã thử gì, hỏi người dùng.

## Checklist mỗi bước

- [ ] Test xanh.
- [ ] Build xanh (`./mvnw verify` / `npm run typecheck`).
- [ ] Demo được hành vi mới.
- [ ] Đã commit với message rõ ràng.
