# Kiểm thử

## Tháp kiểm thử

```
          ┌───────────┐  E2E Playwright - ~5%: luồng sống còn
          ├───────────┤  Integration (Testcontainers, MSW) - ~25%
          ├───────────┤
          │   Unit    │  ~70%: logic thuần, service, mapper, hook, component
          └───────────┘
```

## Yêu cầu tối thiểu

| Phần | Coverage dòng tối thiểu | Ghi chú |
|---|---|---|
| `judge-worker` (`compare/`, `guard/`) | 95% | Logic quyết định verdict - sai là hỏng sản phẩm |
| Tính điểm contest, rating, đóng băng | 90% | |
| Service backend khác | 80% | |
| Frontend `features/*/hooks`, `lib/` | 80% | |
| Component UI | Test hành vi chính, không đặt ngưỡng | |

- Bug fix bắt buộc kèm test tái hiện bug (Prove-It), test phải đỏ trước khi sửa.
- Mỗi endpoint mới có ít nhất: một test thành công, một test validation, một test phân quyền (403/401).
- Thay đổi máy chấm chạy thêm bộ truy vấn tấn công trong `judge-sandbox.md` mục 10.

## Backend

- JUnit 5 + AssertJ + Mockito. Integration test dùng Testcontainers với **MySQL thật**, Kafka thật, container sandbox thật - không dùng H2.
- Đặt tên: `methodName_condition_expectedResult` hoặc `@DisplayName` tiếng Việt.

```java
@Test
@DisplayName("Đúng dữ liệu nhưng sai thứ tự khi đề yêu cầu ORDER BY → OM")
void compare_sameRowsWrongOrder_returnsOrderMismatch() {
    var expected = table(row("Kinh doanh", 32000000), row("Kỹ thuật", 48000000));
    var actual   = table(row("Kỹ thuật", 48000000), row("Kinh doanh", 32000000));

    var verdict = comparator.compare(expected, actual, CompareOptions.ordered());

    assertThat(verdict).isEqualTo(Verdict.OM);
}
```

- Controller test: `@WebMvcTest` + `MockMvc`, kiểm tra status, JSON, và `@PreAuthorize`.
- Repository test: `@DataJpaTest` + Testcontainers MySQL, chạy migration Flyway thật.
- Kafka: kiểm tra producer phát đúng event và consumer idempotent khi nhận trùng.

## Frontend

- Vitest + Testing Library; giả API bằng MSW, không mock `fetch` thủ công.
- Truy vấn phần tử theo vai trò và nhãn (`getByRole('button', { name: 'Submit' })`), không theo class hay test-id trừ khi bất khả kháng.
- Test hành vi người dùng thấy, không test chi tiết cài đặt (state nội bộ, tên hàm).

### Luồng E2E bắt buộc (Playwright)

1. Đăng ký → onboarding → dashboard.
2. Mở bài → Run → Submit sai (WA, thấy diff) → sửa → AC.
3. Đăng ký contest → vào thi → AI bị khoá → nộp bài → thấy mình trên bảng xếp hạng.
4. Setter tạo bài trong Studio → chấm thử → gửi duyệt → admin duyệt → bài xuất hiện trong danh sách.
5. Báo cáo bình luận → moderator xử lý.

## Cấu trúc file

```
backend/<service>/src/test/java/com/sqljudge/<service>/unit/…
backend/<service>/src/test/java/com/sqljudge/<service>/integration/…
frontend/src/features/<feature>/**/<Ten>.test.tsx      # cạnh code
frontend/e2e/<luong>.spec.ts
```

## Lệnh

```bash
./mvnw test                         # unit + integration của một service
./mvnw verify                       # kèm JaCoCo, fail nếu dưới ngưỡng
npm run test                        # Vitest
npm run test -- --coverage
npm run test:e2e                    # Playwright, cần docker compose dev đang chạy
```

Mẫu test chi tiết: `.claude/references/testing-patterns.md`.
