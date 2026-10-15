---
description: Phân tích và sửa một lỗi được báo (GitHub issue, báo cáo QA, báo cáo người dùng) từ đầu đến commit
argument-hint: <số issue, link, hoặc mô tả lỗi>
---

# /fix-issue - Sửa lỗi được báo

Lỗi: **$ARGUMENTS**

## 1. Hiểu lỗi
- Là số issue → `gh issue view $ARGUMENTS` (nếu có `gh`). Đọc báo cáo, bình luận, ảnh, `traceId`, id bài nộp.
- Xác định: màn hình nào (mã canvas), vai trò nào, môi trường nào, mức độ ảnh hưởng.
- Thiếu thông tin để tái hiện → hỏi lại, không đoán.

## 2. Tìm nguyên nhân gốc
Áp dụng `/diagnose`: tái hiện → khoanh vùng → thu nhỏ → tìm gốc.

## 3. Lên phương án
- Nêu nguyên nhân và cách sửa trong 2–4 câu.
- Có nhiều cách → chọn cách nhỏ nhất, an toàn nhất; nêu đánh đổi.
- Lỗi chạm máy chấm/bảo mật hoặc cần đổi schema → trình bày cho người dùng trước khi sửa.

## 4. Sửa theo Prove-It
1. Viết test tái hiện → đỏ.
2. Sửa tại gốc → xanh.
3. Chạy toàn bộ test của service/app.

Lỗi ảnh hưởng dữ liệu đã có (verdict sai, điểm contest sai) → đề xuất thêm bước chấm lại hoặc migration sửa dữ liệu, chờ người dùng duyệt.

## 5. Kiểm tra
- `cd backend && ./mvnw verify` / `cd frontend && npm run lint && npm run typecheck && npm run test`.
- Kiểm tra lại đúng kịch bản trong báo cáo.

## 6. Commit
Commit theo `git-workflow.md` (không tự push), message dạng:

```
fix: <mô tả ngắn>

Closes #<số issue>
```

## Báo cáo
Nguyên nhân, cách sửa, test đã thêm, dữ liệu cần xử lý thêm (nếu có).
