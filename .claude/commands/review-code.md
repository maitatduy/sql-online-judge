---
description: Review thay đổi hiện tại theo 5 trục (đúng, dễ đọc, kiến trúc, bảo mật, hiệu năng) trước khi commit hoặc merge
argument-hint: [nhánh, file, hoặc để trống để review thay đổi chưa commit]
---

# /review-code - Review 5 trục

Phạm vi: **$ARGUMENTS** (để trống = thay đổi chưa commit và commit trên nhánh hiện tại so với `main`).

1. Lấy thay đổi: `git status`, `git diff`, `git diff main...HEAD`.
2. Áp skill **`five-axis-review`** cho toàn bộ thay đổi. Có thể giao cho agent `code-reviewer`.
3. Thay đổi chạm `judge-worker`, `submission-service`, xác thực, phân quyền, AI → thêm skill **`security-audit`** cho phần đó.
4. Chạy kiểm tra tự động của phần bị chạm:

```bash
cd backend && ./mvnw -pl <service> -am verify
cd frontend && npm run lint && npm run typecheck && npm run test
```

## Danh sách kiểm tra nhanh

| Nhóm | Câu hỏi |
|---|---|
| Đúng | Đáp ứng tiêu chí chấp nhận? Trường hợp biên? |
| Test | Có test cho hành vi mới / bug? Test đỏ trước khi sửa? |
| Kiến trúc | Đúng tầng? Không xuyên DB service khác? Không Feign/Kafka trong transaction? |
| Bảo mật | `@PreAuthorize`, kiểm tra sở hữu, validate, không lộ testcase ẩn, không bí mật trong code |
| Máy chấm | Đối chiếu mục 🔴 trong `judge-sandbox.md` |
| DB | Migration mới (không sửa cũ), index cho cột lọc, không N+1 |
| API | Đúng `api-conventions.md`, có OpenAPI annotation, mã trạng thái đúng |
| Frontend | Đủ 4 trạng thái, token đúng `design-system.md`, truy cập bằng bàn phím |

## Kết quả

Theo định dạng của skill `five-axis-review`: tóm tắt, kết luận, các mục **Chặn / Nên sửa / Gợi ý / Khen**, mỗi mục có `file:dòng` và cách sửa.

Không tự sửa code trong lệnh này. Hỏi người dùng có muốn sửa các mục Chặn ngay không.
