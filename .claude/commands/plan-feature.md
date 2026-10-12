---
description: Lập kế hoạch cho một tính năng từ file IDEA - backend plan, frontend plan và danh sách task theo lát cắt dọc
argument-hint: <đường dẫn file docs/ideas/...-idea.md>
---

# /plan-feature - Lập kế hoạch và chia việc

File đặc tả: **$ARGUMENTS**

Không có file IDEA → dừng, đề nghị chạy `/spec` trước.

## Giai đoạn 1: phân tích (chỉ đọc)

- Đọc file IDEA, `docs/ARCHITECTURE.md`, `docs/DESIGN.md`, rule `tech-stack.md`, `api-conventions.md`, `database.md`.
- Rà code hiện có: service nào sở hữu dữ liệu, entity/endpoint/component nào dùng lại được.
- Liệt kê rủi ro (đặc biệt: máy chấm, đồng thời trong contest, phân quyền).

## Giai đoạn 2: backend plan

Tạo `docs/backend-plans/<module>-plan.md` theo `system-design.md`, `database.md`, `api-conventions.md`, `naming-conventions.md` (chạm máy chấm → thêm `judge-sandbox.md`):

```markdown
# Backend plan - <module>

## Service sở hữu
| Service | Vai trò | Lý do |
(mỗi bảng thuộc đúng một service; id service khác chỉ lưu UUID, không FK chéo)

## Database - `sqljudge_<service>`
| Bảng | Cột chính (kiểu MySQL) | Index / unique | Migration `V<n>__...` |

## API
| Method | Path `/api/...` | Role | Request DTO | Response DTO | Mã lỗi |
API nội bộ `/internal/...`: ai gọi, qua Feign + Resilience4j.

## Event Kafka
| Topic `sqljudge.<domain>.<event>` | Producer (outbox) | Consumer (idempotent) | Payload |

## Luồng chính (sơ đồ tuần tự)
## Cache Redis `sqljudge:<service>:...` - TTL, khi nào xoá
## Bảo mật - kiểm tra sở hữu, dữ liệu không được lộ
## Rủi ro và câu hỏi mở
```

Thiếu thông tin ảnh hưởng thiết kế (ai được xem, có tính điểm contest không…) → hỏi người dùng, tối đa 3 câu.

## Giai đoạn 3: frontend plan

Tạo `docs/frontend-plans/<module>-plan.md`:

```markdown
# Frontend plan - <module>

## Route và màn hình
| Route | Mã canvas | Server/Client | Ghi chú |

## Cây component
- `app/(app)/problems/page.tsx` (Server) - lấy trang đầu
  - `ProblemFilters` (Client, SMART) - đọc/ghi query string
  - `ProblemTable` (DUMB) - props: items, onSort
    - `VerdictBadge` (shared)

## State
| Dữ liệu | Nơi giữ | Lý do |
|---|---|---|
| Danh sách bài | TanStack Query `['problems', filters]` | server state |
| Tab đang mở ở workspace | useState | cục bộ |

## Kiểu TypeScript (khớp hợp đồng API trong backend plan)
## Hook cần tạo
## Trạng thái giao diện: tải / trống / lỗi / riêng
```

## Giai đoạn 4: chia task theo lát cắt dọc

Mỗi task là một hành vi hoàn chỉnh (migration → API → giao diện → test) chạy được và demo được. Ưu tiên theo thứ tự: đường chính trước, rủi ro cao trước, hợp đồng API trước.

Lưu `docs/tasks/<module>-todo.md`:

```markdown
# TODO - <module>

## Giai đoạn 1: nền
- [ ] T1 · <hành vi>
  - Service: … · Màn hình: …
  - Xong khi: <tiêu chí kiểm tra được>
  - Test: unit … / integration … / E2E …
  - Phụ thuộc: -

### ✅ Điểm kiểm tra: <điều demo được sau giai đoạn này>

## Giai đoạn 2: tính năng chính
## Giai đoạn 3: hoàn thiện (trạng thái lỗi, responsive, hiệu năng)
```

Quy tắc chia: một task ≤ 1 ngày, ≤ ~300 dòng thay đổi; task lớn hơn thì chia tiếp.

## Giai đoạn 5: trình bày

Tóm tắt cho người dùng: số task, thứ tự, rủi ro, câu hỏi còn mở. Gợi ý:

> Tiếp theo: `/build-slice T1`
