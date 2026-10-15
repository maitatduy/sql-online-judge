# Checklist bảo mật trước khi phát hành

Đánh dấu từng mục. Mục 🔴 chưa đạt → không phát hành.

## Bí mật
- [ ] 🔴 Không có bí mật trong repo (`git log -p | grep -iE "secret|password|api_key"` sạch; dùng gitleaks trong CI).
- [ ] 🔴 Không biến bí mật nào mang tiền tố `NEXT_PUBLIC_`.
- [ ] `.env.example` đủ tên mọi biến, không chứa giá trị thật.
- [ ] JWT key, khoá LLM, mật khẩu DB lấy từ biến môi trường trên server.

## Máy chấm (đối chiếu `judge-sandbox.md`)
- [ ] 🔴 Container sandbox không có mạng ra ngoài; không thấy network ứng dụng.
- [ ] 🔴 judge-worker không có thông tin đăng nhập DB ứng dụng.
- [ ] 🔴 Role chạy SQL có quyền tối thiểu; schema tạm xoá trong `finally`.
- [ ] 🔴 Timeout, giới hạn dòng, giới hạn kích thước kết quả đang bật.
- [ ] 🔴 Bộ truy vấn tấn công chạy trong CI và đều bị chặn.
- [ ] 🔴 Response và log không chứa dữ liệu testcase ẩn.

## Xác thực
- [ ] Access token 15 phút, refresh token cookie `httpOnly; Secure; SameSite=Lax`, xoay vòng, thu hồi khi đăng xuất.
- [ ] BCrypt cost ≥ 12; khoá tạm sau 10 lần đăng nhập sai.
- [ ] OAuth chỉ liên kết theo email đã xác minh.

## Phân quyền
- [ ] Mọi endpoint có `@PreAuthorize` (rà bằng grep các `@*Mapping` không có annotation đi kèm).
- [ ] Đã thử IDOR: đổi id xem code bài nộp người khác, sửa bài viết người khác, xem contest riêng tư.
- [ ] `/admin`, `/studio` chặn ở API bằng `@PreAuthorize` (rào chắn thật); khung `AdminShell` ở client chỉ để không render và không gọi API admin cho người thiếu quyền.
- [ ] Đề contest không trả về trước giờ bắt đầu (giờ server).

## Đầu vào và nội dung
- [ ] Validate mọi request DTO; giới hạn kích thước mã nộp, bài viết, file upload.
- [ ] Markdown render qua sanitize; không `dangerouslySetInnerHTML` chưa lọc.
- [ ] Upload dataset chỉ nhận `.sql`/`.csv`, quét kích thước, không thực thi khi upload.

## AI
- [ ] Khoá LLM chỉ ở `ai-service`.
- [ ] Khoá AI phía server khi đang thi contest có rating.
- [ ] Hạn mức theo gói hoạt động; vượt → 429.
- [ ] Đầu ra AI được kiểm tra, không chứa lời giải trọn vẹn khi chưa AC.

## HTTP và hạ tầng
- [ ] HTTPS bắt buộc; HSTS bật.
- [ ] CSP, `X-Content-Type-Options`, `Referrer-Policy`, `X-Frame-Options` đặt ở gateway/Next.js.
- [ ] CORS chỉ origin frontend.
- [ ] Rate limit tại gateway cho đăng nhập, nộp bài, chạy thử, AI.
- [ ] Swagger UI chỉ bật ở profile `dev`; actuator nằm trên cổng quản trị riêng; Kafka UI không mở ra Internet.
- [ ] Production không bật profile `dev`; `application.yml` không đặt `spring.profiles.default`.
- [ ] Container chạy user không phải root; image có tag cố định.

## Phụ thuộc
- [ ] `./mvnw dependency-check:check` không có High/Critical.
- [ ] `npm audit --omit=dev` không có High/Critical.

## Nhật ký và giám sát
- [ ] Hành động admin (khoá tài khoản, duyệt bài, chấm lại, đổi cấu hình) được ghi nhật ký.
- [ ] Log không chứa mật khẩu, token, cookie, dataset ẩn.
- [ ] Cảnh báo P1 (worker down, hàng đợi tắc) đến đúng người.
