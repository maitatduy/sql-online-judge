# Design system - token và nguyên tắc giao diện

Nguồn thiết kế: canvas https://claude.ai/artifact/S8V1RwGzroBVtYrCi1HYhZ (artboard "Design system", file `sqlj.css`). Nguồn trong code: `frontend/src/styles/globals.css`. Khi hai nơi lệch nhau, sửa cho khớp canvas hoặc hỏi người dùng - không tự chọn.

## Cách khai báo token

Mỗi token màu khai báo ở **3 chỗ** trong `globals.css`:

1. Giá trị nền sáng trong `:root, .light`.
2. Giá trị nền tối trong `.dark`.
3. Ánh xạ `--color-<tên>: var(--<tên>)` trong `@theme inline` để dùng được class Tailwind (`bg-<tên>`, `text-<tên>`).

Trong component chỉ dùng class token (`bg-brand`, `text-verdict-wa`) hoặc `var(--<tên>)` cho thuộc tính SVG. Không viết mã hex trong class hay trong code TypeScript.

## Token màu và ý nghĩa

| Nhóm | Token | Dùng cho |
|---|---|---|
| Thương hiệu | `brand`, `brand-soft` | Hành động chính, link, mục đang chọn |
| Nền và viền | `bg`, `surface`, `surface-2`, `line` | Nền trang, nền thẻ phụ, nền ô trống, viền |
| Chữ | `text`, `text-muted` | Chữ chính, chữ phụ |
| Trợ lý AI | `ai`, `ai-soft` | **Chỉ** các điều khiển và nội dung của trợ lý AI |
| Verdict | `verdict-ac`, `-wa`, `-om`, `-tle`, `-re`, `-fs`, `-pending` | **Chỉ** kết quả chấm bài |
| Độ khó | `difficulty-easy`, `-medium`, `-hard` | **Chỉ** độ khó của bài tập |
| Cú pháp SQL | `syntax-keyword`, `-function`, `-string`, `-number`, `-comment` | Tô màu code |
| Chuỗi ngày | `streak` | Bộ đếm chuỗi ngày học, huy hiệu chuỗi ngày |
| Heatmap hoạt động | `heat-low`, `heat-mid`, `heat-peak` | Mức 1, 2, 4 của lịch hoạt động (mức 0 dùng `surface-2`, mức 3 dùng `brand`) |
| Biểu đồ | `chart-1` | Màu đường và vùng tô của biểu đồ; đường gióng và nhãn phụ dùng `text-muted` |
| Tăng giảm | `trend-up`, `trend-down` | Mức thay đổi của một chỉ số (rating tăng, giảm) |
| Huy hiệu | `badge-contest`, `badge-solved` | Ô huy hiệu về contest và về số bài AC |

Các token của shadcn (`background`, `foreground`, `card`, `primary`, `muted`, `border`, `ring`…) là bí danh trỏ về nhóm trên; đổi giá trị ở token gốc, không đổi ở bí danh.

## Quy tắc dùng màu

- 🔴 Token có ngữ nghĩa (verdict, độ khó, AI) không được mượn cho việc khác, kể cả khi trùng mã màu. Cần màu cho mục đích mới thì khai báo token mới theo đúng tên mục đích.
- Màu không phải tín hiệu duy nhất: verdict có mã chữ, độ khó có chữ, tăng giảm có chữ "Tăng"/"Giảm" kèm mũi tên.
- Nền màu nhạt tạo bằng độ mờ của chính token (`bg-streak/12`), không thêm token `-soft` mới cho từng màu.
- Chữ trên nền phải đạt tương phản 4,5:1 (chi tiết: `.claude/references/accessibility-checklist.md`).

## Chữ

- Giao diện: Be Vietnam Pro (`font-sans`). Code và số liệu: JetBrains Mono (`font-mono`).
- Cỡ chữ nền 14px, dòng 1,5. Tiêu đề khối 16px đậm; tiêu đề trang 22px rất đậm; chữ phụ 12-13px.
- Số liệu nổi bật (thẻ số, rating) dùng `font-mono` rất đậm.

## Bo góc và khoảng cách

- Bo góc: `rounded-sm` 4px, `rounded-md` 6px (nút, ô nhập), `rounded-lg` 10px (thẻ), `rounded-xl` 16px, `rounded-full` (avatar, nhãn tròn).
- Thẻ: viền 1px `border`, nền `bg-card`, đệm 16-24px. Khoảng cách giữa các thẻ 14-24px.
- Vùng chạm tối thiểu 44px ở màn hẹp.

## Trạng thái của một khối

Mỗi khối có dữ liệu phải đủ: đang tải (`Skeleton`, vùng `role="status"`), trống (`EmptyState`: một câu và tối đa một hành động), lỗi (`ErrorState`: có "Thử lại" và mã lỗi khi có), có dữ liệu. Lỗi khi tải lại nền không được che dữ liệu đang hiển thị.
