# Code style

Định dạng do công cụ quyết định, không tranh luận trong review.

## Công cụ

| Phần | Công cụ | Lệnh |
|---|---|---|
| Java | Spotless + google-java-format (AOSP, thụt 4 cách), sortPom cho `pom.xml` | `cd backend && ./mvnw spotless:apply` |
| TypeScript | ESLint (`next/core-web-vitals`, `next/typescript`) | `npm run lint` |
| TS định dạng | Prettier | `npm run format`, kiểm tra: `npm run format:check` |
| SQL migration | Từ khoá viết HOA, 1 cột 1 dòng trong CREATE TABLE | - |

`./mvnw verify` chạy `spotless:check`, sai định dạng là build đỏ. Workflow CI (`.github/workflows/ci.yml`) chạy lại các bước này trên mỗi PR; trước khi mở PR vẫn tự chạy `./mvnw verify` và `npm run format:check && npm run lint && npm run typecheck` để bắt lỗi sớm. Checkstyle, SpotBugs chưa cấu hình; muốn thêm thì hỏi người dùng theo `tech-stack.md`.

## Java

- Độ dài dòng 100 (giới hạn của google-java-format, Spotless tự ngắt dòng).
- Thứ tự trong class: hằng → field → constructor → method public → method private.
- Import tường minh, không dùng `*`.
- Lombok chỉ dùng: `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`, `@NoArgsConstructor`, `@Slf4j`.
- Controller trả `ResponseEntity<T>` khi cần đặt status/header; còn lại trả `T`.

## TypeScript / React

- Độ dài dòng 100, thụt 4 cách, nháy kép, có dấu chấm phẩy, dấu phẩy cuối (cấu hình trong `frontend/.prettierrc`).
- Component là function, khai báo props bằng `interface`, destructuring ngay tại tham số, export có tên (không `export default` trừ file `page.tsx`, `layout.tsx` theo yêu cầu của Next.js).
- Thứ tự import: thư viện ngoài → `@/` nội bộ → tương đối → style. ESLint chưa có rule tự sắp xếp, nên giữ thứ tự này khi viết.
- Class Tailwind: dùng token đã khai báo (`bg-brand`, `text-verdict-wa`), không viết mã hex tuỳ ý trong class. Ghép class điều kiện bằng `cn()`.
- Một component một file; file vượt ~200 dòng thì tách.

## Chữ trên giao diện

- Tiếng Việt có dấu đầy đủ, không viết tắt khó hiểu.
- Dùng dấu gạch nối `-`, không dùng gạch dài `—`.
- Viết hoa chữ cái đầu câu, không Title Case kiểu tiếng Anh ("Danh sách bài tập", không "Danh Sách Bài Tập").
- Số dùng dấu chấm phân cách hàng nghìn (`12.380`), dấu phẩy thập phân (`41,2%`). Dùng `Intl.NumberFormat('vi-VN')`.
- Ngày giờ hiển thị theo giờ Việt Nam; thời gian tương đối dùng "5 phút trước".
- Tên verdict giữ mã gốc (AC, WA…) kèm giải thích tiếng Việt ở tooltip.

## Commit và PR

Xem `git-workflow.md`.
