---
description: Làm gọn code đã có mà không đổi hành vi - có test bảo vệ, đổi từng bước nhỏ
argument-hint: <file, class, hoặc thư mục cần làm gọn>
---

# /simplify-code - Làm gọn không đổi hành vi

Phạm vi: **$ARGUMENTS** (để trống = các file vừa sửa trên nhánh hiện tại).

## Nguyên tắc

- **Hàng rào của Chesterton**: chưa hiểu vì sao code tồn tại thì chưa xoá. Đọc git blame, test, comment trước.
- **Hành vi giữ nguyên.** Không sửa bug, không thêm tính năng trong cùng lần này - ghi lại để làm riêng.
- **Có test bảo vệ trước.** Phần cần làm gọn chưa có test → viết test mô tả hành vi hiện tại trước.
- Mỗi lần thay đổi ≤ 500 dòng; lớn hơn chia làm nhiều lượt.

## Quy trình

1. **Xác định mục tiêu**: hàm dài, lồng sâu, trùng lặp, tên khó hiểu, class làm nhiều việc.
2. **Hiểu trước khi đổi**: đọc test, chạy test, ghi lại hành vi biên.
3. **Liệt kê cơ hội** và trình bày cho người dùng nếu thay đổi lớn.
4. **Áp dụng từng bước**, chạy test sau mỗi bước.
5. **Xác nhận**: `./mvnw verify` / `npm run lint && npm run typecheck && npm run test` xanh, kết quả giống trước.
6. **Test đỏ** sau một bước → hoàn tác bước đó, không sửa test cho khớp.

## Những cách làm gọn hay dùng

| Vấn đề | Cách |
|---|---|
| `if` lồng nhiều tầng | Guard clause, trả sớm |
| Hàm dài | Tách hàm có tên nói rõ ý định |
| `switch`/`if` theo dialect rải rác | Đa hình: mỗi dialect một class `Sandbox` |
| Điều kiện nghiệp vụ dài | Đưa thành method của entity (`contest.isRunningAt(now)`) |
| Toán tử ba ngôi lồng | `if` rõ ràng hoặc bảng tra |
| Map tay DTO lặp lại | MapStruct |
| Component React > 200 dòng | Tách component con theo vùng giao diện |
| `useEffect` đồng bộ state dẫn xuất | Tính trực tiếp khi render (`useMemo` nếu tốn) |
| Code chết, import thừa, flag hết hạn | Xoá |

## Không làm

- Không đổi API công khai, tên bảng, tên topic Kafka trong lần làm gọn.
- Không đổi định dạng hàng loạt ngoài phạm vi.
- Không "làm gọn" phần bảo mật của sandbox để bớt kiểm tra.

## Kết quả

Liệt kê thay đổi theo từng bước, số dòng giảm, xác nhận test xanh, và các vấn đề phát hiện được nhưng để lại cho lần sau.
