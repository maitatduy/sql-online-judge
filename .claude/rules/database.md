# Database - MySQL 8, Spring Data JPA, Flyway

> Phạm vi: DB **ứng dụng** của các service. Sandbox chấm bài có quy tắc riêng ở `judge-sandbox.md`.

## Nguyên tắc chung

- Mỗi service một database (`sqljudge_problem`, `sqljudge_contest`…). Không service nào đọc/ghi DB của service khác.
- Không có khoá ngoại hay quan hệ JPA xuyên service. Tham chiếu sang service khác là một cột UUID thường, tên kết thúc bằng `_id` (`user_id`, `problem_id`); lấy dữ liệu đầy đủ qua OpenFeign hoặc giữ bản sao nhỏ cập nhật bằng event Kafka.
- Schema chỉ thay đổi qua Flyway. `spring.jpa.hibernate.ddl-auto=validate` ở mọi môi trường.

## Khoá chính và cột chung

- Khoá chính UUID, sinh dạng **UUIDv7** (tăng dần theo thời gian, index B-tree không bị phân mảnh), lưu `BINARY(16)`.
- Mọi bảng kế thừa `BaseEntity` của `common-lib`: `id`, `created_at`, `updated_at`, `created_by`, `updated_by` (dùng Spring Data Auditing).
- Dữ liệu quan trọng dùng soft delete với `deleted_at` (bài tập, bài nộp, bài viết, người dùng). Dữ liệu phụ thuộc không có giá trị độc lập (lựa chọn quiz, dòng chi tiết) được xoá thật theo bản ghi cha.
- Bảng có cập nhật đồng thời (standings, quota, problem stats) có cột `version` và `@Version` để khoá lạc quan.

## Flyway

- File: `src/main/resources/db/migration/V<n>__<mo_ta_snake_case>.sql`. Lấy số lớn nhất hiện có + 1.
- 🔴 Không sửa migration đã chạy ở bất kỳ môi trường nào. Muốn đổi: tạo migration mới.
- Mỗi migration làm một việc. Đổi tên/xoá cột theo 2 bước (thêm mới → chuyển dữ liệu → bản phát hành sau mới xoá) để không làm hỏng instance đang chạy bản cũ.
- Dữ liệu seed cho dev đặt trong `db/seed/` và chỉ chạy với profile `dev`.

## Entity

```java
@Entity
@Table(name = "problems", indexes = {
    @Index(name = "idx_problems_status_difficulty", columnList = "status, difficulty"),
    @Index(name = "uk_problems_slug", columnList = "slug", unique = true)
})
@Getter @Setter @NoArgsConstructor
public class Problem extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String slug;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProblemStatus status;

    @Column(name = "author_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID authorId;              // tham chiếu auth-service, không phải quan hệ JPA

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Testcase> testcases = new ArrayList<>();
}
```

- Không dùng `@Data` của Lombok trên entity (equals/hashCode trên quan hệ gây lỗi và vòng lặp).
- Quan hệ mặc định `FetchType.LAZY`; `@ManyToOne` phải ghi rõ `fetch = LAZY`.
- Enum lưu `EnumType.STRING`, không dùng `ORDINAL`.
- Tên bảng/cột khai báo tường minh, không phụ thuộc naming strategy.

## Truy vấn

- Chỉ lấy cột cần thiết cho danh sách: dùng projection (interface hoặc record) thay vì tải cả entity.
- Phân trang bằng `Pageable`; danh sách công khai luôn có giới hạn `size`.
- Tránh N+1: dùng `@EntityGraph` hoặc `JOIN FETCH` có chủ đích; bật `hibernate.generate_statistics` khi debug để đếm câu lệnh.
- Query native: luôn dùng tham số (`:name`), không nối chuỗi.
- Bảng thống kê lớn (đếm AC, tỉ lệ AC theo bài) cập nhật dồn qua event, không `COUNT(*)` trên bảng `submissions` mỗi lần mở trang.

## Transaction

- `@Transactional` đặt ở method của service, không đặt ở controller hay repository.
- Method chỉ đọc: `@Transactional(readOnly = true)`.
- Không gọi Feign hay chờ Kafka **bên trong** transaction DB. Phát event sau khi commit bằng **outbox**: ghi bản ghi vào bảng `outbox_events` cùng transaction, một tác vụ nền đọc và publish lên Kafka.

## Index

- Đánh index cho cột dùng trong `WHERE`, `ORDER BY`, `JOIN` thường xuyên; index ghép theo thứ tự lọc → sắp xếp.
- Unique index cho dữ liệu không được trùng (`users.email`, `users.username`, `problems.slug`, `contest_registrations(contest_id, user_id)`).
- Khai báo index trong migration **và** trong `@Table(indexes=…)`.

## Đặt tên

| Thứ | Quy ước | Ví dụ |
|---|---|---|
| Bảng | snake_case, số nhiều | `submissions`, `contest_registrations` |
| Cột | snake_case | `runtime_ms`, `first_failed_case` |
| Boolean | `is_` / `has_` | `is_public`, `has_editorial` |
| Index | `idx_<bảng>_<cột>` | `idx_submissions_user_id_created_at` |
| Unique | `uk_<bảng>_<cột>` | `uk_users_email` |
| Khoá ngoại | `fk_<bảng>_<bảng_tham_chiếu>` | `fk_testcases_problems` |

## Bảo mật dữ liệu

- Không log entity chứa mật khẩu băm, token, email đầy đủ.
- Mật khẩu băm bằng BCrypt (cost ≥ 12) ở `auth-service`; không service nào khác thấy cột mật khẩu.
- Đáp án chuẩn và dataset testcase ẩn: chỉ role `SETTER` sở hữu bài, `MODERATOR`, `ADMIN` được đọc.
