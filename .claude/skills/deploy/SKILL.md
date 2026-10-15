---
name: deploy
description: Triển khai SQL Judge lên staging hoặc production - kiểm tra, build image, chạy migration, deploy từng service, kiểm tra sức khoẻ, rollback khi lỗi. Chỉ chạy khi người dùng gọi /deploy.
disable-model-invocation: true
argument-hint: <staging|production> [tên service, mặc định tất cả]
---

# Triển khai

Môi trường và phạm vi: **$ARGUMENTS**

Quy trình đầy đủ (CI/CD, cấu trúc VPS, docker compose, rollback): xem [deploy.md](deploy.md).

Repo hiện chưa có Dockerfile, `infra/docker-compose.prod.yml` và workflow trong `.github/workflows/`. Thiếu thứ nào thì dừng, báo người dùng và đề nghị dựng phần đó trước, không deploy tay theo cách khác.

## Bước 0: xác nhận

- Production: hỏi người dùng xác nhận rõ ràng trước khi làm bất cứ bước nào có tác dụng.
- 🔴 Không deploy production khi đang có contest diễn ra hoặc bắt đầu trong vòng 2 giờ (kiểm tra `GET /api/contests?status=RUNNING,UPCOMING`). Nếu có → dừng, báo người dùng.

## Bước 1: trạng thái sạch

```bash
git status                  # không có thay đổi chưa commit
git fetch && git status     # nhánh đã cập nhật với remote
```

Production chỉ deploy từ tag `vX.Y.Z` trên `main`.

## Bước 2: kiểm tra

```bash
(cd backend && ./mvnw verify)
(cd frontend && npm run format:check && npm run lint && npm run typecheck && npm run test)
```

CI trên GitHub phải xanh cho commit sẽ deploy. Không bỏ qua test.

## Bước 3: build và đẩy image

Thường do workflow `deploy.yml` làm khi push tag. Chạy tay chỉ khi CI lỗi hạ tầng. Build context là `backend/` vì service cần parent POM và `common-lib`:

```bash
docker build -f backend/<service>/Dockerfile -t ghcr.io/<org>/sqljudge-<service>:<tag> backend
docker push ghcr.io/<org>/sqljudge-<service>:<tag>
```

## Bước 4: migration

- Flyway chạy khi service khởi động. Migration phải tương thích ngược với phiên bản đang chạy (chỉ thêm; xoá cột ở bản phát hành sau).
- Production: sao lưu database của service có migration mới trước khi deploy.

## Bước 5: deploy theo thứ tự

1. `config-server`, `discovery-server` (nếu đổi)
2. Service nghiệp vụ có migration
3. Các service nghiệp vụ còn lại, `judge-worker` (worker cũ hoàn tất job đang chấm rồi mới dừng)
4. `api-gateway`
5. `frontend`

```bash
ssh deploy@<host> "cd /opt/sqljudge && IMAGE_TAG=<tag> docker compose -f docker-compose.prod.yml up -d --no-deps <service>"
```

## Bước 6: kiểm tra sức khoẻ

```bash
curl -fsS https://<domain>/api/health
ssh deploy@<host> "cd /opt/sqljudge && docker compose -f docker-compose.prod.yml ps"
```

- Readiness mọi service `UP`.
- Smoke test: mở trang chủ, đăng nhập tài khoản test, Run và Submit một bài mẫu → nhận verdict.
- Theo dõi 5 phút trên Grafana: tỉ lệ 5xx, hàng đợi chấm, lỗi sandbox.

## Rollback

Lỗi ở bước 6 → quay về tag trước ngay:

```bash
ssh deploy@<host> "cd /opt/sqljudge && IMAGE_TAG=<tag-trước> docker compose -f docker-compose.prod.yml up -d --no-deps <service>"
```

Migration không tự rollback. Nếu migration là nguyên nhân: dừng, báo người dùng, khôi phục từ bản sao lưu theo quyết định của người dùng.

## Báo cáo

Môi trường, tag, service đã deploy, kết quả kiểm tra sức khoẻ, có rollback hay không.
