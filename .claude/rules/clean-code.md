# Clean code - Java và TypeScript

## Đặt tên có nghĩa

```java
// ❌
int d; List<Submission> list1;
if (s.getV() == 1) { … }

// ✅
int elapsedMinutes; List<Submission> acceptedSubmissions;
if (submission.getVerdict() == Verdict.AC) { … }
```

- Cùng một khái niệm dùng cùng một từ trong toàn hệ thống: `submission` (không lẫn `attempt`, `solution`), `problem` (không lẫn `task`, `exercise`), `testcase` (không lẫn `test`, `case`).
- Không dùng số "ma thuật": `if (runtimeMs > 2000)` → `if (runtimeMs > problem.getTimeLimitMs())`.
- Không lặp ngữ cảnh: trong class `Contest` gọi là `startAt`, không phải `contestStartAt`.

## Hàm

- Một hàm làm một việc, đọc được trong một màn hình (gợi ý ≤ 30 dòng).
- Tối đa 3 tham số; nhiều hơn thì gom thành record/đối tượng.
- Không dùng tham số cờ boolean để rẽ nhánh hành vi: tách `submit()` và `run()` thay vì `execute(boolean isSubmit)`.
- Ưu tiên guard clause, tránh lồng `if` quá 2 cấp.
- Hàm truy vấn không có tác dụng phụ; hàm thay đổi trạng thái đặt tên bằng động từ.

```java
// ✅ guard clause
public void register(UUID contestId, UUID userId) {
    Contest contest = contestRepository.findById(contestId).orElseThrow(...);
    if (contest.hasStarted()) throw new BusinessRuleException(CONTEST_ALREADY_STARTED, "Contest đã bắt đầu");
    if (registrationRepository.exists(contestId, userId)) return;
    registrationRepository.save(ContestRegistration.of(contest, userId));
}
```

## Đóng gói điều kiện nghiệp vụ

Đặt luật nghiệp vụ vào method có tên trên đối tượng sở hữu nó:

```java
// ❌ rải rác trong service
if (now.isAfter(c.getStartAt()) && now.isBefore(c.getEndAt()) && c.getStatus() != CANCELLED)

// ✅
if (contest.isRunningAt(now))
```

## Bất biến và kiểu dữ liệu

- DTO, event, kết quả so khớp dùng `record` (Java) hoặc `type`/`readonly` (TS).
- Dùng enum/union cho tập giá trị cố định (`Verdict`, `Difficulty`, `Dialect`), không dùng chuỗi tự do.
- Java: không trả `null` cho collection - trả danh sách rỗng; dùng `Optional` cho kết quả tìm kiếm ở repository/service, không dùng `Optional` làm field hay tham số.
- TypeScript: `strict: true`, không dùng `any`; dữ liệu từ API đi qua kiểu đã khai báo trong `features/*/types.ts`.

## SOLID áp dụng thực tế

- **S**: `ResultComparator` chỉ so khớp; `SandboxPool` chỉ quản lý sandbox; không gộp.
- **O**: thêm dialect mới bằng cách thêm một class `Sandbox` mới, không sửa `if/else` theo dialect rải rác.
- **L**: mọi `Sandbox` thay thế được cho nhau qua cùng interface.
- **I**: interface nhỏ, đúng nhu cầu người dùng nó.
- **D**: service phụ thuộc interface (`Clock`, `SandboxFactory`) để test được; tiêm qua constructor (`@RequiredArgsConstructor`), không dùng `@Autowired` trên field.

## Thời gian

- Không gọi `LocalDateTime.now()` hay `new Date()` trong logic nghiệp vụ. Java tiêm `Clock`; frontend truyền "bây giờ" vào hàm thuần khi cần test.
- Lưu và truyền thời gian ở UTC (`Instant`); chỉ đổi sang `Asia/Ho_Chi_Minh` khi hiển thị.

## Bất đồng bộ

- Java: không `Thread.sleep` để chờ kết quả; dùng event, `CompletableFuture` có timeout, hoặc virtual thread cho I/O chặn trong worker.
- TypeScript: `async/await`, không trộn `.then()`; mọi Promise hoặc được `await`, hoặc được xử lý lỗi rõ ràng.

## Comment

- Comment giải thích **tại sao**, không giải thích code làm gì.
- Không để code bị comment-out; xoá đi, git đã lưu lịch sử.
- `TODO` phải kèm việc cụ thể và người/issue phụ trách: `// TODO(#142): hỗ trợ SQL Server`.

## Xoá code chết

Hàm, biến, import, feature flag không còn dùng phải xoá ngay trong cùng PR.
