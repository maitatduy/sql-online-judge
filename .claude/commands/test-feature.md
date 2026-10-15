---
description: Viết và chạy test cho một tính năng (TDD) hoặc tái hiện một bug bằng test (Prove-It)
argument-hint: <tính năng, file, hoặc mô tả bug>
---

# /test-feature - Kiểm thử

Đối tượng: **$ARGUMENTS**

Xác định trước: đây là **tính năng mới** hay **bug**?

## Tính năng mới - RED → GREEN → REFACTOR

1. **RED**: viết test mô tả hành vi mong đợi theo tiêu chí chấp nhận. Chạy, xác nhận đỏ **đúng lý do** (không phải lỗi biên dịch hay thiếu import).
2. **GREEN**: viết code tối thiểu để xanh.
3. **REFACTOR**: làm gọn khi test vẫn xanh.

## Bug - Prove-It

1. Viết test tái hiện bug bằng đúng dữ liệu trong báo cáo (id bài nộp, truy vấn, dataset).
2. Chạy - phải đỏ. Xanh → chưa tái hiện được, quay lại phân tích.
3. Sửa tại nguyên nhân gốc (dùng `/diagnose` nếu chưa rõ).
4. Test xanh, chạy toàn bộ test của service.

## Chọn loại test

| Đối tượng | Loại | Công cụ |
|---|---|---|
| So khớp kết quả, tính điểm, rating, guard | Unit | JUnit 5 + AssertJ, `@ParameterizedTest` |
| Repository, migration | Integration | `@DataJpaTest` + Testcontainers MySQL |
| Controller + phân quyền | Slice | `@WebMvcTest` + MockMvc |
| Sandbox, bộ truy vấn tấn công | Integration | Testcontainers PostgreSQL/MySQL |
| Hook, component | Unit/Component | Vitest + Testing Library + MSW |
| Luồng người dùng | E2E | Playwright |

## Viết test tốt

- **AAA**: Arrange - Act - Assert, ngăn bằng dòng trống.
- **Một hành vi một test.** Tên test nói rõ điều kiện và kết quả; `@DisplayName` tiếng Việt được khuyến khích.
- **DAMP hơn DRY**: test dễ đọc riêng lẻ quan trọng hơn bớt lặp.
- **Không phụ thuộc thứ tự, giờ hệ thống, mạng ngoài.** Dùng `Clock` cố định, dữ liệu tự tạo.
- **Ưu tiên đối tượng thật/fake** hơn mock; mock chỉ ranh giới ngoài (LLM, email).

## Chạy

```bash
cd backend && ./mvnw -pl judge-worker -am test -Dtest=ResultComparatorTest -Dsurefire.failIfNoSpecifiedTests=false
cd backend && ./mvnw verify                   # mọi module, kèm Spotless và JaCoCo
cd frontend && npm run test -- src/features/workspace
cd frontend && npm run test:coverage
cd frontend && npx playwright test e2e/submit-flow.spec.ts   # khi đã cài Playwright
```

## Checklist

- [ ] Test đỏ trước khi sửa (đã chứng kiến).
- [ ] Bao phủ trường hợp biên liên quan (NULL, rỗng, trùng, hết giờ, không đủ quyền).
- [ ] Toàn bộ test của service/app xanh.
- [ ] Coverage không giảm dưới ngưỡng trong `testing.md`.

Mẫu chi tiết: `.claude/references/testing-patterns.md`.
