# Quy ước API

Áp dụng cho mọi API REST công khai qua `api-gateway` và API nội bộ giữa các service.

## URL

- Tiền tố `/api`, tài nguyên số nhiều, kebab-case. Không thêm `/v1` (chưa versioning theo URL).
- Lồng tối đa 2 cấp: `/api/contests/{contestId}/problems`. Sâu hơn thì dùng query param.
- Hành động không phải CRUD: động từ ở cuối, kebab-case: `POST /api/submissions/{id}/rejudge`, `POST /api/problems/{id}/publish`.
- API nội bộ giữa service: `/internal/...`, gateway **không** định tuyến ra ngoài.
- SSE: `GET /api/submissions/{id}/events`, `GET /api/contests/{id}/standings/events`.

| Đúng | Sai |
|---|---|
| `GET /api/problems?topic=join&difficulty=MEDIUM` | `GET /api/getProblems` |
| `POST /api/problems/{id}/submissions` | `POST /api/submitProblem` |
| `GET /api/users/{username}` | `GET /api/user_detail/{username}` |

## Phương thức và mã trạng thái

| Method | Dùng cho | Thành công |
|---|---|---|
| GET | Đọc | 200 |
| POST | Tạo mới, hành động | 201 (tạo, kèm header `Location`) / 202 (đã nhận, xử lý nền - ví dụ nộp bài) / 200 |
| PUT | Thay toàn bộ | 200 |
| PATCH | Sửa một phần | 200 |
| DELETE | Xoá (thường là soft delete) | 204 |

| Lỗi | Khi nào |
|---|---|
| 400 | Dữ liệu không hợp lệ (validation) |
| 401 | Chưa đăng nhập, token hết hạn |
| 403 | Đủ đăng nhập nhưng thiếu quyền, hoặc AI bị khoá trong contest |
| 404 | Không có tài nguyên (hoặc không được phép biết nó tồn tại) |
| 409 | Xung đột: trùng username, contest đã bắt đầu, dữ liệu đã bị người khác sửa |
| 422 | Đúng định dạng nhưng sai nghiệp vụ (nộp bài khi contest đã kết thúc) |
| 429 | Vượt rate limit hoặc hạn mức AI - kèm header `Retry-After` |
| 500 | Lỗi không lường trước. Không bao giờ chủ động trả 500 |

Nộp bài trả **202** cùng `submissionId`; verdict đến qua SSE hoặc `GET /api/submissions/{id}`.

## Định dạng response

### Thành công - trả DTO trực tiếp, không bọc

```json
{ "id": "0192…", "slug": "highest-salary-per-department", "title": "Lương cao nhất mỗi phòng ban", "difficulty": "MEDIUM" }
```

### Danh sách có phân trang

```json
{
  "items": [ … ],
  "page": 1,
  "size": 20,
  "totalItems": 330,
  "totalPages": 17
}
```

- `page` bắt đầu từ 1, `size` mặc định 20, tối đa 100.
- Feed dài và realtime (bài nộp gần nhất, bình luận) dùng cursor: `?cursor=<opaque>&size=20` → `{ "items": [], "nextCursor": "…" }`.

### Lỗi - một định dạng duy nhất (`ApiError` trong common-lib)

```json
{
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "Dữ liệu gửi lên không hợp lệ",
  "errors": [ { "field": "title", "message": "Không được để trống" } ],
  "traceId": "4bf92f3577b34da6",
  "timestamp": "2026-10-11T13:05:22Z"
}
```

- `message` bằng tiếng Việt, hiển thị được cho người dùng.
- `code` là hằng số tiếng Anh UPPER_SNAKE (`PROBLEM_NOT_FOUND`, `CONTEST_ALREADY_STARTED`, `AI_QUOTA_EXCEEDED`, `AI_DISABLED_DURING_CONTEST`) để frontend xử lý theo nhánh.
- Không bao giờ trả stack trace, câu SQL nội bộ, tên schema sandbox.

## Đặt tên trường

- JSON dùng camelCase. Enum dùng UPPER_SNAKE (`MEDIUM`, `LEARNER`, `POSTGRESQL`). Riêng verdict dùng mã ngắn `AC`, `WA`, `OM`, `TLE`, `RE`, `FS`, `SE` như CLAUDE.md mục 6.
- Thời gian: chuỗi ISO-8601 UTC (`2026-10-11T12:30:00Z`). Frontend tự đổi sang giờ Việt Nam.
- Thời lượng: số nguyên kèm đơn vị trong tên (`runtimeMs`, `durationMinutes`).
- Id: UUID dạng chuỗi. URL công khai cho bài, khoá học, contest dùng `slug`.

## Lọc, sắp xếp, tìm kiếm

- Lọc: `?difficulty=MEDIUM&topic=join&status=SOLVED` (nhiều giá trị: `topic=join,window`).
- Sắp xếp: `?sort=acceptanceRate,desc`.
- Tìm kiếm: `?q=lương`.
- Chỉ cho phép lọc/sắp xếp trên các trường có index - liệt kê rõ trong backend plan.

## Xác thực và phân quyền

- Header `Authorization: Bearer <accessToken>`; refresh token nằm trong cookie `httpOnly; Secure; SameSite=Lax`.
- Gateway xác thực chữ ký JWT; service kiểm tra lại quyền bằng `@PreAuthorize` theo role: `LEARNER`, `SETTER`, `MODERATOR`, `ADMIN`.
- Quyền theo sở hữu (chỉ sửa bài viết của mình, chỉ xem bài nộp của mình) kiểm tra trong service, không chỉ dựa vào role.

## Idempotency và rate limit

- `POST` có tác dụng phụ quan trọng (nộp bài, đăng ký contest) chấp nhận header `Idempotency-Key`.
- Rate limit tại gateway (Redis): nộp bài 6/phút/người, chạy thử 20/phút/người, AI theo hạn mức gói, đăng nhập 10/phút/IP.

## Tài liệu

- Mọi endpoint có chú thích springdoc (`@Operation`, `@ApiResponse`), xem tại `/swagger-ui` của từng service (tắt ở production).
- Thay đổi phá vỡ hợp đồng (đổi tên trường, đổi kiểu) phải cập nhật frontend plan và báo rõ trong PR.
