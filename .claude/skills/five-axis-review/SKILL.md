---
name: five-axis-review
description: Khung review code 5 trục (đúng, dễ đọc, kiến trúc, bảo mật, hiệu năng) áp cho SQL Judge. Dùng khi review một thay đổi, một PR, hoặc tự rà code vừa viết trước khi commit.
---

# Review code 5 trục

> "Duyệt khi thay đổi chắc chắn làm codebase tốt hơn, dù chưa hoàn hảo."

## Quy trình

1. **Hiểu bối cảnh**: task trong `docs/tasks/`, plan liên quan, mã màn hình nếu là giao diện.
2. **Đọc test trước**: test nói lên ý định; thiếu test là phát hiện đầu tiên.
3. **Đọc code** theo 5 trục.
4. **Phân loại** phát hiện theo mức độ.
5. **Góp ý hành động được**: `file:dòng`, vấn đề, vì sao, cách sửa.

## Trục 1 - Đúng

- Đáp ứng đủ tiêu chí chấp nhận?
- Trường hợp biên: rỗng, NULL, trùng, đồng thời (hai lần nộp cùng lúc), biên thời gian (đúng giây contest kết thúc), múi giờ.
- Verdict, tính điểm, phạt, đóng băng, rating có test cho từng nhánh?
- Xử lý lỗi đúng `error-handling.md`; không nuốt exception.

## Trục 2 - Dễ đọc

- Tên đúng từ vựng miền; hàm ngắn; ít lồng; không cờ boolean.
- Không code chết, không comment trong code (kể cả `TODO`).
- Frontend: component < ~200 dòng, props rõ kiểu.

## Trục 3 - Kiến trúc

- Đúng tầng (`project-structure.md`); controller mỏng; không trả Entity.
- Không truy cập DB service khác; không Feign/Kafka trong transaction; event qua outbox.
- Không thêm công nghệ ngoài `tech-stack.md`.
- Frontend: không import chéo `features/`; `'use client'` đặt thấp nhất có thể; gọi API qua `api-client`.

## Trục 4 - Bảo mật (soi kỹ nhất)

- Chạm máy chấm → đối chiếu từng mục 🔴 của `judge-sandbox.md`.
- `@PreAuthorize` + kiểm tra sở hữu; thử nghĩ cách đổi id để xem dữ liệu người khác.
- Validate đầu vào; SQL ứng dụng có tham số.
- Không lộ testcase ẩn, đáp án, lỗi nội bộ qua response/log/AI.
- Không bí mật trong code hoặc `NEXT_PUBLIC_*`.

## Trục 5 - Hiệu năng

- N+1, thiếu index, tải cả entity cho danh sách.
- Việc nặng đồng bộ trong request.
- Cache có TTL và được xoá khi dữ liệu đổi.
- Frontend: Monaco import động; workspace không render thừa; bundle không phình.

## Kích thước thay đổi

| Dòng thay đổi | Đánh giá |
|---|---|
| < 200 | Tốt |
| 200–400 | Chấp nhận |
| > 400 | Đề nghị tách |

## Mức độ góp ý

| Nhãn | Nghĩa |
|---|---|
| **Chặn** | Phải sửa trước khi merge |
| **Nên sửa** | Quan trọng, nên sửa trong PR này |
| **Gợi ý** | Tuỳ chọn |
| **Khen** | Điểm làm tốt - luôn có ít nhất một |

## Định dạng kết quả

```markdown
## Review: <tiêu đề>

**Tóm tắt:** …
**Kết luận:** Duyệt | Duyệt sau khi sửa mục Chặn | Cần làm lại

### Chặn
- `path/File.java:42` - vấn đề. Vì sao. Cách sửa.
### Nên sửa
### Gợi ý
### Khen
```

## Quy tắc

- Góp ý về code, không về người viết.
- Không bắt bẻ định dạng (Spotless/Prettier lo).
- Không đòi làm lại toàn bộ khi sửa nhỏ là đủ.
- Không tự sửa code khi chưa được yêu cầu.
