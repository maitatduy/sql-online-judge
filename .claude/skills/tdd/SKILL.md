---
name: tdd
description: Phát triển hướng kiểm thử (RED-GREEN-REFACTOR) và mẫu Prove-It cho bug. Dùng trước khi viết logic nghiệp vụ mới, khi sửa bug, hoặc khi được yêu cầu viết test cho backend Spring Boot hay frontend Next.js của SQL Judge.
---

# Test-Driven Development

Test viết trước là bản đặc tả chạy được. Với SQL Judge, chỗ sai nhỏ nhất (NULL, thứ tự dòng, phút phạt) ảnh hưởng tới điểm thật của người học - nên logic đó phải được test dẫn dắt.

## Khi nào dùng

- Logic quyết định kết quả: so khớp verdict, StatementGuard, tính điểm và phạt contest, đóng băng, rating, hạn mức AI.
- Mọi bug fix (Prove-It).
- Endpoint mới: test thành công + validation + phân quyền.
- Hook và component có logic hiển thị theo trạng thái (workspace, bảng xếp hạng).

Không bắt buộc TDD cho: cấu hình, migration thuần, component chỉ trình bày không logic - nhưng vẫn cần test sau.

## Chu trình

### RED - viết một test đỏ
- Một hành vi, tên test nói rõ điều kiện → kết quả.
- Chạy và **chứng kiến** nó đỏ vì đúng lý do (giá trị sai), không phải lỗi biên dịch.

### GREEN - code tối thiểu
- Viết đủ để test xanh, không hơn. Chấp nhận code chưa đẹp.

### REFACTOR - làm gọn khi đang xanh
- Đặt tên lại, tách hàm, bỏ trùng lặp ở cả code và test.
- Chạy lại test sau mỗi thay đổi nhỏ.

Lặp lại với hành vi tiếp theo.

## Prove-It cho bug

1. Test tái hiện bằng đúng dữ liệu của báo cáo (truy vấn, dataset, thời điểm nộp).
2. Đỏ → xác nhận đã tái hiện. Xanh → chưa hiểu bug, quay lại phân tích.
3. Sửa tại nguyên nhân gốc.
4. Xanh; chạy toàn bộ test của service.
5. Giữ test lại vĩnh viễn - nó là hàng rào chống tái phát.

## Ví dụ - phạt thời gian contest

```java
@Test
@DisplayName("Phạt = phút AC + 20 phút cho mỗi lần sai trước đó; lỗi cú pháp không tính")
void penalty_countsOnlyWrongAttemptsBeforeAccepted() {
    var attempts = List.of(
        attempt(5, Verdict.RE),    // không tính
        attempt(12, Verdict.WA),   // +20
        attempt(24, Verdict.AC),   // 24
        attempt(30, Verdict.WA));  // sau AC, không tính

    assertThat(PenaltyCalculator.forProblem(attempts)).isEqualTo(Duration.ofMinutes(44));
}
```

Viết test này → đỏ (chưa có `PenaltyCalculator`) → cài đặt tối thiểu → xanh → thêm test cho "chưa AC thì không phạt" → …

## Tháp kiểm thử và công cụ

| Lớp | Tỉ lệ | Backend | Frontend |
|---|---|---|---|
| Unit | ~70% | JUnit 5, AssertJ, `@ParameterizedTest` | Vitest |
| Integration | ~25% | Testcontainers (MySQL, Kafka, sandbox), `@WebMvcTest` | Testing Library + MSW |
| E2E | ~5% | - | Playwright |

## DAMP hơn DRY trong test

Mỗi test đọc riêng phải hiểu được. Lặp lại vài dòng Arrange còn hơn giấu dữ liệu quan trọng sau helper chung. Dùng test data builder (`aContest().startAt(...)`) để vừa gọn vừa rõ.

## Test double - thứ tự ưu tiên

Thật → Fake (Testcontainers, `Clock.fixed`, repository trong bộ nhớ) → Stub → Mock. Mock chỉ cho ranh giới ngoài: LLM, email, Feign tới service khác.

## Anti-pattern

- Viết code trước, viết test sau cho "đủ coverage".
- Sửa test cho khớp code sai.
- Test phụ thuộc giờ hệ thống, mạng ngoài, thứ tự chạy.
- `Thread.sleep` để chờ Kafka - dùng Awaitility.
- Mock chính class đang test.
- Dùng H2 thay MySQL.

## Checklist hoàn thành

- [ ] Mỗi test mới đã từng đỏ.
- [ ] Trường hợp biên đã nghĩ tới (rỗng, NULL, trùng, biên thời gian, không đủ quyền).
- [ ] Toàn bộ test của service/app xanh.
- [ ] Coverage đạt ngưỡng trong `.claude/rules/testing.md`.
