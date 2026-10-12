---
description: Thi công một task theo lát cắt dọc bằng TDD - database, API, giao diện, test - giữ hệ thống luôn chạy được
argument-hint: <mã task, ví dụ T3, hoặc đường dẫn file todo>
---

# /build-slice - Thi công từng lát cắt

Task: **$ARGUMENTS**

## Điều kiện

- Có `docs/tasks/<module>-todo.md` và task này còn chưa xong. Không có → đề nghị `/plan-feature`.
- Các task phụ thuộc đã xong.

## Với mỗi task

### 1. Nạp ngữ cảnh
Đọc mô tả task, backend plan, frontend plan, design brief liên quan. Đọc code sẽ bị chạm.

### 2. Viết test trước (RED)
Theo skill `tdd`: viết test cho tiêu chí "Xong khi" của task, chạy và xác nhận đỏ đúng lý do.

### 3. Thi công tối thiểu (GREEN), theo thứ tự
1. **Dữ liệu** - migration Flyway `V<n>__...sql`, entity kế thừa `BaseEntity`, repository. Theo `database.md`.
2. **API** - DTO (`record` + validation), mapper MapStruct, service (`@Transactional`, event qua outbox), controller mỏng có `@PreAuthorize`. Theo `api-conventions.md`, `error-handling.md`, `security.md`.
3. **Giao diện** - trang App Router (Server Component mặc định), component trong `features/<module>/`, hook TanStack Query, form react-hook-form + zod, chỉ dùng token trong `docs/DESIGN.md`, đủ 4 trạng thái tải/trống/lỗi/không quyền. Theo `project-structure.md`, `code-style.md`.

Bỏ qua tầng nào task không cần.

Chỉ viết đủ để test xanh. Chạm vào chấm bài → đối chiếu `judge-sandbox.md` từng mục.

### 4. Làm gọn (REFACTOR)
Đặt tên lại, tách hàm, xoá trùng lặp - test vẫn xanh.

### 5. Kiểm tra
```bash
./mvnw verify                                   # service bị chạm
npm run lint && npm run typecheck && npm run test   # nếu chạm frontend
```

### 6. Đánh dấu và báo cáo
- Tick task trong file todo.
- Báo người dùng: file đã tạo/sửa, test đã thêm, cách tự kiểm tra (URL, màn hình).
- Task cuối của module xong → thêm một mục vào đầu `docs/FEATURES_DONE.md` (ngày, tên tính năng, tóm tắt dưới 50 từ, việc còn lại).
- Gợi ý commit lát cắt này theo `git-workflow.md`; không tự push.

## Quy tắc

- Một task một lần. Không "tiện tay" làm task khác.
- Mỗi bước kết thúc ở trạng thái build được, test xanh.
- Chỉ sửa file liên quan. Không đổi định dạng hàng loạt, không đổi tên ngoài phạm vi.
- Phần chưa xong mà phải merge: ẩn sau feature flag.

## Khi bị kẹt

- Test đỏ lạ hoặc build hỏng → `/diagnose`.
- Plan sai hoặc thiếu (thiếu trường API, thiếu bảng) → dừng, báo người dùng, đề xuất cập nhật plan. Không tự bịa.
- Sau 2 lần thử không được → dừng và trình bày những gì đã thử.

## Dấu hiệu sai

- Hơn ~300 dòng thay đổi cho một task.
- Viết xong cả backend lẫn frontend mới chạy test lần đầu.
- Sửa test cho khớp code sai.
