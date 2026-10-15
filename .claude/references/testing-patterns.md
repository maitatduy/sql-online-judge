# Mẫu kiểm thử - tra cứu nhanh

## Cấu trúc AAA

```java
@Test
@DisplayName("Đăng ký contest đã bắt đầu → lỗi CONTEST_ALREADY_STARTED")
void register_afterStart_throws() {
    // Arrange
    Clock clock = Clock.fixed(Instant.parse("2026-10-17T12:31:00Z"), ZoneOffset.UTC);
    Contest contest = aContest().startAt("2026-10-17T12:30:00Z").build();
    var service = new ContestRegistrationService(repoWith(contest), clock);

    // Act + Assert
    assertThatThrownBy(() -> service.register(contest.getId(), USER_ID))
        .isInstanceOf(BusinessRuleException.class)
        .extracting("code").isEqualTo(ErrorCode.CONTEST_ALREADY_STARTED);
}
```

## Unit - so khớp kết quả (bảng trường hợp)

```java
static Stream<Arguments> cases() {
    return Stream.of(
        arguments("khớp hoàn toàn",       t(r(1,"a")), t(r(1,"a")), unordered(), Verdict.AC),
        arguments("thiếu một dòng",        t(r(1,"a"),r(2,"b")), t(r(1,"a")), unordered(), Verdict.WA),
        arguments("dòng trùng phải đếm",   t(r(1,"a"),r(1,"a")), t(r(1,"a")), unordered(), Verdict.WA),
        arguments("sai thứ tự khi có ORDER BY", t(r(1),r(2)), t(r(2),r(1)), ordered(), Verdict.OM),
        arguments("NULL khác chuỗi 'NULL'", t(r((Object) null)), t(r("NULL")), unordered(), Verdict.WA),
        arguments("số thực trong sai số",  t(r(0.3)), t(r(0.1 + 0.2)), unordered(), Verdict.AC),
        arguments("khác số cột",           t(r(1,"a")), t(r(1)), unordered(), Verdict.WA),
        arguments("cả hai rỗng",           t(), t(), unordered(), Verdict.AC)
    );
}
```

## Integration - repository với MySQL thật

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Testcontainers
class SubmissionRepositoryTest {
    @Container @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired SubmissionRepository repository;

    @Test
    void findRecentByUser_returnsNewestFirst() { … }
}
```

## Integration - sandbox với truy vấn tấn công

```java
@ParameterizedTest
@ValueSource(strings = {
    "DROP TABLE employees",
    "SELECT pg_read_file('/etc/passwd')",
    "COPY employees TO PROGRAM 'id'",
    "SELECT * FROM pg_catalog.pg_authid",
    "WITH RECURSIVE r(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM r) SELECT * FROM r",
    "SELECT pg_sleep(60)",
    "SELECT 1; DROP TABLE employees"
})
void maliciousQuery_isBlockedOrHarmless(String sql) {
    JudgeResult result = judge.run(problemWithEmployees(), Dialect.POSTGRESQL, sql);

    assertThat(result.verdict()).isIn(Verdict.FS, Verdict.RE, Verdict.TLE, Verdict.WA);
    assertThat(datasetStillIntact()).isTrue();
    assertThat(noSandboxSchemasLeft()).isTrue();
}
```

## Controller - phân quyền

```java
@WebMvcTest(ProblemAdminController.class)
class ProblemAdminControllerTest {
    @Test @WithMockUser(roles = "LEARNER")
    void approve_asLearner_forbidden() throws Exception {
        mvc.perform(post("/api/problems/{id}/approve", ID)).andExpect(status().isForbidden());
    }
}
```

## Frontend - component với MSW

```tsx
it('hiện WA và cho xem khác biệt', async () => {
  server.use(http.get('*/api/submissions/:id', () => HttpResponse.json(waSubmission)));
  render(<ResultPanel submissionId="s1" />, { wrapper: QueryWrapper });

  expect(await screen.findByText('Wrong Answer')).toBeInTheDocument();
  await userEvent.click(screen.getByRole('tab', { name: 'So sánh' }));
  expect(screen.getByRole('row', { name: /Hoàng Quốc Bảo/ })).toBeInTheDocument();
});
```

## E2E - Playwright

```ts
test('thí sinh không dùng được AI khi đang thi', async ({ page }) => {
  await loginAs(page, 'contestant');
  await page.goto('/contests/sql-weekly-37/problems/B');
  await expect(page.getByRole('button', { name: /AI tắt trong contest/ })).toBeDisabled();
});
```

## Test double - thứ tự ưu tiên

1. Đối tượng thật (record, logic thuần).
2. Fake: Testcontainers, repository trong bộ nhớ, `Clock.fixed`.
3. Stub: trả giá trị cố định cho Feign client.
4. Mock (Mockito/vi.fn): chỉ cho ranh giới ngoài - LLM, email - và khi cần kiểm tra có gọi hay không.

## Đặt tên

- Java: `method_condition_expected` + `@DisplayName` tiếng Việt.
- TS: `it('hiện … khi …')`.
- File: `XxxTest.java` cho cả unit (package `unit`) và integration (package `integration`) - Surefire chỉ chạy `*Test`, chưa cấu hình Failsafe cho `*IT`; `xxx.test.tsx`, `xxx.spec.ts` (E2E).

## Không nên

- Test phụ thuộc thứ tự chạy hoặc dữ liệu của test khác.
- `Thread.sleep` / `waitForTimeout` để chờ - dùng chờ theo điều kiện (`Awaitility`, `expect(...).toBeVisible()`).
- Assert quá nhiều chi tiết cài đặt (số lần gọi hàm nội bộ).
- Snapshot cả trang lớn.
- Dùng H2 thay MySQL.
