# Bảo mật - quy tắc bắt buộc

> Ưu tiên cao nhất cùng với `judge-sandbox.md`. Vi phạm mục 🔴 là lỗi chặn merge.

## 🔴 Không bao giờ

1. Commit hoặc đọc file bí mật: `.env`, `.env.local`, `.env.production`, `application-secret.yml`, `application-prod.yml`, khoá, chứng chỉ.
2. Hard-code mật khẩu, khoá API, JWT secret, khoá LLM trong code hoặc trong `NEXT_PUBLIC_*`.
3. Nối chuỗi để tạo câu SQL trên DB ứng dụng. Luôn dùng tham số.
4. Chạy SQL của người dùng ngoài sandbox (xem `judge-sandbox.md`).
5. Trả stack trace, câu SQL nội bộ, tên schema sandbox, dữ liệu testcase ẩn ra response.
6. Log mật khẩu, token, cookie, mã OTP, nội dung dataset ẩn.
7. Tin vai trò hoặc userId gửi từ client. Danh tính lấy từ JWT đã xác thực.
8. Tắt kiểm tra CSRF/CORS/TLS "cho nhanh".

## Xác thực

- Access token JWT ký RS256, sống 15 phút; refresh token sống 14 ngày, lưu cookie `httpOnly; Secure; SameSite=Lax`, xoay vòng mỗi lần dùng, thu hồi khi đăng xuất (Redis blacklist theo `jti`).
- Mật khẩu: BCrypt cost ≥ 12, tối thiểu 8 ký tự, kiểm tra với danh sách mật khẩu phổ biến.
- OAuth GitHub/Google qua Spring Security OAuth2 Client; liên kết tài khoản theo email đã xác minh.
- Khoá tạm tài khoản sau 10 lần đăng nhập sai trong 15 phút.

## Phân quyền

- Gateway xác thực token; **mỗi service kiểm tra lại** bằng `@PreAuthorize`. Không endpoint nào thiếu kiểm tra.
- Kiểm tra quyền sở hữu trong service: người dùng chỉ xem code bài nộp của mình (trừ khi đã AC và bài nộp được công khai), chỉ sửa bài viết của mình, setter chỉ sửa bài của mình.
- Admin console (`/admin`) yêu cầu `ADMIN`; trang báo cáo vi phạm cho thêm `MODERATOR`.
- Trả 404 thay vì 403 khi việc lộ sự tồn tại của tài nguyên là thông tin nhạy cảm (contest riêng tư, bài chưa xuất bản).

## Kiểm tra đầu vào

- Backend: Jakarta Bean Validation trên mọi request DTO (`@NotBlank`, `@Size`, `@Pattern`…), `@Valid` ở controller.
- Frontend: zod để báo lỗi sớm - **không thay thế** kiểm tra ở backend.
- Giới hạn kích thước: mã nộp 64 KB, bài viết 50 KB, file dataset upload 20 MB, chỉ nhận `.sql` và `.csv`.

## Nội dung do người dùng tạo (bài viết, bình luận, đề bài)

- Markdown render phía client bằng thư viện có sanitize (rehype-sanitize). Không dùng `dangerouslySetInnerHTML` với nội dung chưa lọc.
- Khối SQL "chạy được" trong bài viết luôn chạy qua sandbox, dataset của bài được gắn - không cho người viết bài chỉ định dataset tuỳ ý.
- Link ngoài: `rel="noopener noreferrer nofollow ugc"`.

## Trợ lý AI

- Khoá LLM chỉ nằm trong `ai-service`.
- Nội dung người dùng gửi cho AI coi là dữ liệu, không phải chỉ thị. Prompt hệ thống ghi rõ chính sách (không đưa đáp án đầy đủ trước AC, chỉ chủ đề SQL), và kiểm tra lại đầu ra: chặn câu trả lời chứa khối SQL giải trọn bài khi người dùng chưa AC.
- Không gửi cho LLM: đáp án chuẩn (khi chưa AC), dataset ẩn, email hay thông tin cá nhân khác của người dùng.
- AI bị khoá phía server khi người dùng đang thi contest có rating (kiểm tra ở `ai-service`, không chỉ ẩn nút).

## Contest và gian lận

- Đề contest chỉ trả về khi contest đã bắt đầu (kiểm tra theo giờ server).
- Thời điểm nộp lấy theo giờ server.
- Trong giờ thi: khoá lời giải công khai và thảo luận của các bài trong contest.
- Ghi nhận tín hiệu nghi gian lận (nhiều tài khoản cùng IP, code giống nhau bất thường) để admin xem - không tự động khoá.

## HTTP

- Header bảo mật tại gateway và Next.js: `Strict-Transport-Security`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`, `Content-Security-Policy` (cho phép worker của Monaco), `X-Frame-Options: DENY`.
- CORS chỉ cho origin của frontend; không dùng `*` khi có credentials.
- Rate limit tại gateway (xem `api-conventions.md`).

## Phụ thuộc

- `./mvnw dependency-check:check` (OWASP) và `npm audit --omit=dev` chạy trong CI; lỗ hổng mức High/Critical chặn merge.
- Dependabot bật cho Maven, npm, GitHub Actions, Docker.
- Image Docker dùng bản base chính thức có tag cụ thể, chạy bằng user không phải root.
