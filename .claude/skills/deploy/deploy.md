# Quy trình triển khai SQL Judge

## Tổng quan

```
Dev (máy cá nhân, docker compose dev)
  │  push nhánh, mở PR
  ▼
GitHub Actions - ci.yml: build, lint, test, kiểm tra bảo mật
  │  merge vào main
  ▼
deploy.yml - build image → GHCR → deploy staging tự động
  │  tạo tag vX.Y.Z
  ▼
deploy.yml - deploy production (cần duyệt thủ công trong GitHub Environment)
  ▼
VPS: docker compose prod  ·  Prometheus + Grafana + Loki theo dõi
```

Giai đoạn đầu: staging và production mỗi môi trường một VPS. Sandbox chấm bài chạy trên cùng VPS nhưng trong network Docker riêng, không có Internet. Khi tải tăng, tách `judge-worker` + sandbox sang máy riêng trước tiên.

---

## 1. Môi trường dev

```bash
git clone <repo> && cd sql-judge
cp .env.example .env                          # điền giá trị dev
docker compose -f infra/docker-compose.dev.yml up -d   # MySQL, Kafka, Redis, sandbox, Prometheus, Grafana
cd backend/<service> && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

Seed dữ liệu dev: profile `dev` chạy `db/seed/` của từng service (người dùng mẫu, 20 bài, 1 khoá học, 1 contest).

---

## 2. CI/CD trên GitHub

### Bảo vệ nhánh `main`
- Bắt buộc PR, CI xanh, ít nhất 1 review (khi có người review).
- Cấm force push, cấm xoá nhánh.

### `.github/workflows/ci.yml` (mỗi PR và push)

```yaml
name: ci
on: [pull_request, push]
jobs:
  backend:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service: [common-lib, api-gateway, auth-service, problem-service, learning-service,
                  submission-service, judge-worker, contest-service, community-service,
                  ai-service, notification-service]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '17', cache: maven }
      - run: ./mvnw -B -pl ${{ matrix.service }} -am verify
        working-directory: backend
  frontend:
    runs-on: ubuntu-latest
    defaults: { run: { working-directory: frontend } }
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '22', cache: npm, cache-dependency-path: frontend/package-lock.json }
      - run: npm ci
      - run: npm run lint && npm run typecheck && npm run test -- --coverage
      - run: npm audit --omit=dev --audit-level=high
  security:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 }
      - uses: gitleaks/gitleaks-action@v2
```

Chỉ build lại service có thay đổi (dùng `dorny/paths-filter`) khi CI chậm.

### `.github/workflows/deploy.yml`

```yaml
name: deploy
on:
  push:
    branches: [main]          # → staging
    tags: ['v*.*.*']          # → production
jobs:
  build:
    runs-on: ubuntu-latest
    permissions: { contents: read, packages: write }
    strategy:
      matrix: { service: [api-gateway, auth-service, problem-service, …, frontend] }
    steps:
      - uses: actions/checkout@v4
      - uses: docker/login-action@v3
        with: { registry: ghcr.io, username: ${{ github.actor }}, password: ${{ secrets.GITHUB_TOKEN }} }
      - uses: docker/build-push-action@v6
        with:
          context: ${{ matrix.service == 'frontend' && 'frontend' || format('backend/{0}', matrix.service) }}
          push: true
          tags: |
            ghcr.io/${{ github.repository_owner }}/sqljudge-${{ matrix.service }}:${{ github.sha }}
            ghcr.io/${{ github.repository_owner }}/sqljudge-${{ matrix.service }}:${{ github.ref_type == 'tag' && github.ref_name || 'main' }}
  deploy:
    needs: build
    runs-on: ubuntu-latest
    environment: ${{ github.ref_type == 'tag' && 'production' || 'staging' }}   # production cần duyệt tay
    steps:
      - uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.DEPLOY_HOST }}
          username: deploy
          key: ${{ secrets.DEPLOY_SSH_KEY }}
          script: |
            cd /opt/sqljudge
            export IMAGE_TAG=${{ github.sha }}
            docker compose -f docker-compose.prod.yml pull
            docker compose -f docker-compose.prod.yml up -d --remove-orphans
            ./scripts/healthcheck.sh
```

Bí mật (`DEPLOY_HOST`, `DEPLOY_SSH_KEY`, khoá LLM, mật khẩu DB) lưu trong GitHub Environments, không trong repo.

---

## 3. Image trên GHCR

- Tên: `ghcr.io/<org>/sqljudge-<service>`.
- Tag: SHA commit (bất biến, dùng để deploy/rollback), `main` (bản staging mới nhất), `vX.Y.Z` (bản phát hành).
- Dockerfile backend: build nhiều tầng, chạy `eclipse-temurin:21-jre` với user không phải root, `JAVA_OPTS` giới hạn heap theo container.
- Dockerfile frontend: `output: 'standalone'` của Next.js, chạy `node:22-alpine` non-root.
- Image sandbox (`infra/sandbox/`): build riêng, cấu hình quyền và giới hạn sẵn trong image.

---

## 4. VPS

### Cấu trúc thư mục

```
/opt/sqljudge/
├── docker-compose.prod.yml
├── .env                       # quyền 600, chỉ user deploy đọc được
├── scripts/
│   ├── healthcheck.sh
│   ├── backup-mysql.sh        # chạy cron hằng đêm, giữ 14 bản
│   └── rollback.sh
├── monitoring/                # prometheus.yml, alert rules, dashboard
└── backups/
```

### Phần chính của `docker-compose.prod.yml`

```yaml
networks:
  app: {}
  sandbox:
    internal: true             # 🔴 không có route ra Internet

services:
  mysql:
    image: mysql:8.4
    networks: [app]
    volumes: [mysql-data:/var/lib/mysql]
  kafka:
    image: apache/kafka:3.8.0
    networks: [app]
  redis:
    image: redis:7.4-alpine
    networks: [app]

  submission-service:
    image: ghcr.io/<org>/sqljudge-submission-service:${IMAGE_TAG}
    networks: [app]
    env_file: .env
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8080/actuator/health/readiness"]

  judge-worker:
    image: ghcr.io/<org>/sqljudge-judge-worker:${IMAGE_TAG}
    networks: [app, sandbox]   # nói chuyện với Kafka (app) và sandbox; KHÔNG có biến MySQL ứng dụng
    deploy: { replicas: 2 }
    stop_grace_period: 40s     # hoàn tất job đang chấm

  sandbox-postgres:
    image: ghcr.io/<org>/sqljudge-sandbox-postgres:${SANDBOX_TAG}
    networks: [sandbox]        # 🔴 chỉ ở network sandbox
    deploy: { resources: { limits: { cpus: '2', memory: 2g } } }
  sandbox-mysql:
    image: ghcr.io/<org>/sqljudge-sandbox-mysql:${SANDBOX_TAG}
    networks: [sandbox]

  api-gateway:
    image: ghcr.io/<org>/sqljudge-api-gateway:${IMAGE_TAG}
    networks: [app]
  frontend:
    image: ghcr.io/<org>/sqljudge-frontend:${IMAGE_TAG}
    networks: [app]
  caddy:                       # TLS tự động, reverse proxy cho frontend và gateway
    image: caddy:2
    ports: ["80:80", "443:443"]
    networks: [app]
```

Chỉ `caddy` mở cổng ra ngoài. MySQL, Kafka, Redis, actuator, Grafana không mở cổng công khai (Grafana truy cập qua SSH tunnel hoặc sau xác thực).

---

## 5. Rollback

```bash
ssh deploy@<host>
cd /opt/sqljudge
IMAGE_TAG=<sha-hoặc-tag-trước> docker compose -f docker-compose.prod.yml up -d --no-deps <service>
./scripts/healthcheck.sh
```

- Rollback image mất khoảng 30 giây.
- Migration Flyway chỉ cộng thêm và tương thích ngược, nên bản cũ vẫn chạy được trên schema mới.
- Dữ liệu hỏng do migration: khôi phục từ `backups/` theo quyết định của người dùng, sau khi dừng service liên quan.

---

## 6. Lịch phát hành

- Không deploy production trong lúc contest diễn ra hoặc 2 giờ trước giờ bắt đầu.
- Ưu tiên deploy sáng ngày thường; tránh tối thứ Bảy (giờ SQL Weekly).
- Bản sửa khẩn trong contest: chỉ khi lỗi chặn việc chấm; thông báo cho thí sinh qua banner của ban tổ chức trước.
