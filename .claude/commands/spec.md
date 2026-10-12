---
description: Viết đặc tả (IDEA) cho một tính năng trước khi code - hỏi rõ yêu cầu rồi lưu vào docs/ideas/
argument-hint: <tên tính năng hoặc mô tả ngắn>
---

# /spec - Đặc tả trước khi code

Tính năng cần đặc tả: **$ARGUMENTS**

## Bước 1: nạp ngữ cảnh

- Đọc `docs/ARCHITECTURE.md`, `docs/FEATURES_DONE.md`, và các file trong `docs/ideas/` để tránh trùng hoặc mâu thuẫn.
- Tìm màn hình liên quan trong canvas (mã 6.x / A.x) - ghi trong `docs/DESIGN.md`.

## Bước 2: hỏi những gì còn thiếu

Chỉ hỏi điều chưa rõ, tối đa 5 câu một lượt, ưu tiên câu ảnh hưởng nhiều nhất:

- **Mục tiêu**: giải quyết vấn đề gì, cho vai trò nào (LEARNER, SETTER, MODERATOR, ADMIN)?
- **Phạm vi**: hành động chính, dữ liệu cần lưu, cái gì **không** làm lần này?
- **Quy tắc nghiệp vụ**: giới hạn, trạng thái, ai được làm gì, khi nào (trước/trong/sau contest)?
- **Máy chấm**: tính năng có chạy SQL người dùng không? Dialect nào? Có dùng testcase ẩn không?
- **AI**: AI có tham gia không, bị khoá khi nào?

## Bước 3: viết file `docs/ideas/<module>-idea.md`

```markdown
# <Tên tính năng>

## Mục tiêu
## Người dùng và vai trò
## User story
- Là <vai trò>, tôi muốn <hành động>, để <giá trị>.

## Tiêu chí chấp nhận
- [ ] Cho …, khi …, thì …

## Quy tắc nghiệp vụ
## Dữ liệu chính
## Màn hình liên quan (mã canvas)
## Ràng buộc bảo mật
(bắt buộc nếu chạm máy chấm, dữ liệu cá nhân, quyền admin, AI)

## Ngoài phạm vi
## Câu hỏi còn mở

## Ranh giới cho Claude
- Luôn: …
- Hỏi trước: …
- Không bao giờ: …
```

## Bước 4: xác nhận

Tóm tắt trong 3–5 dòng cho người dùng, liệt kê câu hỏi còn mở. Khi được đồng ý, gợi ý bước tiếp theo:

> Tiếp theo: `/plan-feature docs/ideas/<module>-idea.md`

## Không làm

- Không viết code, không tạo migration ở bước này.
- Không tự quyết quy tắc nghiệp vụ quan trọng (tính điểm, quyền) khi người dùng chưa xác nhận.
