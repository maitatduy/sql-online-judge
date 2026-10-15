# Checklist khả năng truy cập (WCAG 2.1 AA)

## Bàn phím

- [ ] Mọi chức năng dùng được bằng bàn phím; thứ tự Tab hợp lý (trái → phải, trên → dưới).
- [ ] Focus luôn nhìn thấy (vòng focus 2px màu brand, không `outline: none` trơn).
- [ ] Có link "Bỏ qua tới nội dung chính" ở đầu trang.
- [ ] Workspace: phím tắt `Ctrl+Enter` (Run), `Ctrl+Shift+Enter` (Submit) có hiển thị trên nút; `Esc` thoát khỏi editor để Tab tiếp được (Monaco bẫy phím Tab - bật chế độ `tabFocusMode` bằng `Ctrl+M` và ghi chú cho người dùng).
- [ ] Modal/drawer (AI, xác nhận) giữ focus bên trong, `Esc` để đóng, trả focus về nút đã mở.

## Trình đọc màn hình

- [ ] HTML ngữ nghĩa: `header`, `nav`, `main`, `aside`, `footer`; một `h1` mỗi trang, tiêu đề không nhảy cấp.
- [ ] Nút có tên: nút chỉ có icon phải có `aria-label` tiếng Việt ("Thông báo", "Đóng", "Gửi").
- [ ] Bảng kết quả truy vấn là `<table>` thật, có `<th scope="col">`.
- [ ] Verdict mới được thông báo qua vùng `aria-live="polite"` ("Đã chấm xong: Accepted, 8 trên 8 testcase").
- [ ] Đồng hồ contest không thông báo mỗi giây; chỉ thông báo các mốc (còn 5 phút, còn 1 phút).
- [ ] Biểu đồ (radar kỹ năng, rating, heatmap) có `role="img"` + `aria-label` tóm tắt, hoặc bảng dữ liệu thay thế.
- [ ] `lang="vi"` trên `<html>`.

## Thị giác

- [ ] Tương phản chữ ≥ 4.5:1; chữ ≥ 24px hoặc ≥ 18,66px đậm ≥ 3:1; thành phần giao diện ≥ 3:1.
- [ ] Không dùng màu là tín hiệu duy nhất: verdict có chữ (AC/WA), dòng sai trong diff có icon, độ khó có chữ.
- [ ] Phóng to 200% không vỡ bố cục, không cuộn ngang (trừ bảng trong vùng cuộn riêng).
- [ ] Tôn trọng `prefers-reduced-motion` (pháo giấy AC, chuyển cảnh).
- [ ] Dark mode workspace đạt cùng mức tương phản.

## Form

- [ ] Mọi input có `<label>` liên kết; placeholder không thay label.
- [ ] Lỗi hiển thị cạnh field, liên kết bằng `aria-describedby`, `aria-invalid="true"`; không chỉ đổi màu viền.
- [ ] Gửi form lỗi → focus vào field lỗi đầu tiên.
- [ ] Trường bắt buộc đánh dấu bằng chữ, không chỉ dấu `*`.

## Thành phần tương tác

| Thành phần | Yêu cầu |
|---|---|
| Nút vs link | Điều hướng dùng `<a>`/`<Link>`; hành động dùng `<button>` |
| Tab (Đề bài / Schema / Lời giải) | `role="tablist"`, mũi tên trái/phải chuyển tab |
| Menu thả xuống | Mở bằng Enter/Space, di chuyển bằng mũi tên, đóng bằng Esc |
| Switch (cài đặt) | `role="switch"` + `aria-checked` |
| Toast | `role="status"`; lỗi quan trọng dùng `role="alert"` |
| Vùng chạm mobile | ≥ 44 × 44 px |

## Công cụ

```bash
npx @axe-core/cli http://localhost:3000/problems
npx playwright test --project=a11y        # khi đã cài Playwright và @axe-core/playwright
```

Kiểm tra thủ công: chỉ dùng bàn phím cho luồng nộp bài; bật NVDA hoặc VoiceOver đọc trang bài tập và nghe thông báo verdict.

## Lỗi hay gặp

- `div` có `onClick` thay vì `button`.
- Icon không có nhãn.
- Focus mất sau khi đóng modal.
- Thông báo verdict chỉ bằng màu.
- Trạng thái đang tải không có chữ cho trình đọc màn hình (`aria-busy`, "Đang tải…").
