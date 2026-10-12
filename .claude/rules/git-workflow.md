# Git workflow

## Nhánh (trunk-based gọn nhẹ)

| Nhánh | Mục đích |
|---|---|
| `main` | Luôn build được, luôn deploy được. Được bảo vệ: chỉ merge qua PR, CI xanh |
| `feat/<phạm-vi>-<mô-tả>` | Tính năng: `feat/judge-order-mismatch`, `feat/web-workspace-ai-panel` |
| `fix/<phạm-vi>-<mô-tả>` | Sửa lỗi: `fix/contest-freeze-penalty` |
| `chore/…`, `docs/…`, `refactor/…` | Việc khác |

Nhánh sống ngắn (≤ 3 ngày). Tính năng lớn chia lát cắt dọc nhỏ, merge dần, phần chưa xong ẩn sau feature flag.

## Commit - Conventional Commits, tiếng Anh

```
<type>(<scope>): <mô tả ngắn, thể mệnh lệnh, ≤ 72 ký tự>

[thân: vì sao thay đổi, nếu cần]

[footer: Closes #123, BREAKING CHANGE: …]
```

| type | Dùng cho |
|---|---|
| `feat` | Tính năng mới |
| `fix` | Sửa lỗi |
| `refactor` | Đổi cấu trúc, không đổi hành vi |
| `test` | Chỉ thêm/sửa test |
| `docs` | `docs/`, `.claude/`, README |
| `chore` | Build, dependency, cấu hình |
| `perf` | Cải thiện hiệu năng |
| `ci` | GitHub Actions |

**scope** là tên service hoặc khu vực: `auth`, `problem`, `learning`, `submission`, `judge`, `contest`, `community`, `ai`, `notification`, `gateway`, `web`, `admin`, `infra`.

Ví dụ:

```
feat(judge): return OM verdict when rows match but order differs
fix(contest): stop updating public standings after freeze time
feat(web): add AI hint panel to workspace
docs(claude): translate rules to Vietnamese
```

Một commit kể một câu chuyện. Không gộp thay đổi không liên quan; stage theo từng file (`git add <path>`), không `git add -A`.

Trước khi commit: `git status`, không stage `.env*` (trừ `.env.example`), `application-secret.yml`, `application-prod.yml`, `*.pem`, `*.key`, `settings.local.json`, `CLAUDE.local.md`. Không dùng `--no-verify`. Trước khi push: liệt kê commit sẽ push và hỏi người dùng.

## Pull request

- Tiêu đề theo định dạng commit. Mô tả gồm: làm gì, vì sao, cách kiểm tra, ảnh chụp nếu đổi giao diện (kèm mã màn hình, ví dụ "6.6 Workspace").
- Dưới ~400 dòng thay đổi (không tính file sinh tự động). Lớn hơn thì tách.
- CI bắt buộc xanh: build, lint, test, coverage, kiểm tra bảo mật phụ thuộc.
- Thay đổi chạm `judge-worker`, `security` hoặc migration cần review kỹ trục bảo mật (`/review-code`).
- Merge bằng **squash**; xoá nhánh sau khi merge.

## Không được

- Force push lên `main` hoặc nhánh người khác đang dùng.
- Sửa lịch sử đã đẩy lên remote.
- Commit file bí mật (xem `security.md`). Nếu lỡ commit: thu hồi bí mật ngay, rồi mới xoá khỏi lịch sử.
- Push khi chưa được người dùng xác nhận.

## Phát hành

- Tag `vMAJOR.MINOR.PATCH` trên `main` kích hoạt workflow deploy production.
- Changelog sinh tự động từ commit message.
