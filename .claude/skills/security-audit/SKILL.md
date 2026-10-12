---
name: security-audit
description: Rà soát bảo mật theo checklist của SQL Judge - máy chấm/sandbox, xác thực, phân quyền, dữ liệu testcase, AI, bí mật, phụ thuộc. Dùng trước khi merge thay đổi nhạy cảm hoặc trước mỗi lần phát hành.
---

# Rà soát bảo mật

Phạm vi mặc định: thay đổi trên nhánh hiện tại so với `main`. Có thể chỉ định thư mục hoặc service.

## Bước 1: thu thập

```bash
git diff main...HEAD --stat
git diff main...HEAD
```

Xác định thay đổi chạm vùng nào: máy chấm, xác thực, phân quyền, nội dung người dùng, AI, hạ tầng.

## Bước 2: kiểm tra theo mức

### 🔴 Nghiêm trọng - kiểm tra đầu tiên
- [ ] Không bí mật trong diff (khoá, mật khẩu, token, `NEXT_PUBLIC_*` nhạy cảm).
- [ ] SQL người dùng chỉ chạy trong sandbox; role quyền thấp; schema tạm dọn trong `finally`; timeout và giới hạn dòng còn nguyên.
- [ ] Không đường nào lộ testcase ẩn hoặc đáp án chuẩn (response, log, thông báo lỗi, prompt AI).
- [ ] Endpoint mới đều có `@PreAuthorize`; endpoint admin yêu cầu `ADMIN`/`MODERATOR`.
- [ ] SQL trên DB ứng dụng dùng tham số.

### 🟡 Cao
- [ ] Kiểm tra quyền sở hữu (IDOR) cho tài nguyên theo id.
- [ ] Validate mọi request DTO, giới hạn kích thước.
- [ ] Markdown/HTML người dùng được sanitize.
- [ ] AI bị khoá phía server khi đang thi; hạn mức áp đúng.
- [ ] Đề contest không lộ trước giờ.

### 🟢 Trung bình
- [ ] Rate limit cho endpoint mới có tác dụng phụ.
- [ ] Header bảo mật, CORS không nới.
- [ ] Log không chứa dữ liệu nhạy cảm.

### ℹ️ Thấp
- [ ] Thông báo lỗi không lộ phiên bản/thư viện.
- [ ] Phụ thuộc mới có license phù hợp, đang được bảo trì.

## Bước 3: chạy công cụ

```bash
./mvnw dependency-check:check
npm audit --omit=dev
gitleaks detect --source . --no-banner        # nếu đã cài
grep -rEn "(password|secret|api[_-]?key|token)\s*[:=]\s*['\"][^'\"]+" --include=*.java --include=*.ts --include=*.tsx --include=*.yml .
```

Thay đổi máy chấm → chạy integration test bộ truy vấn tấn công (`judge-sandbox.md` mục 10).

## Bước 4: báo cáo

```markdown
# Báo cáo bảo mật - <phạm vi> - <ngày>

**Kết luận:** An toàn để merge | Cần sửa trước khi merge

## Nghiêm trọng
- **[NT-1] <tiêu đề>** - `file:dòng`. Tác động. Cách sửa.
## Cao
## Trung bình / Thấp
## Đã kiểm tra, không phát hiện vấn đề
```

Checklist đầy đủ trước phát hành: `.claude/references/security-checklist.md`. Rà sâu, lập mô hình mối đe doạ: giao cho agent `security-auditor`.
