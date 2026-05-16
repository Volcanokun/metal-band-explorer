# metal-band-explorer

メタルにはサブジャンルがあまりにも多すぎるため、好きなメタルバンドを入力すると類似のバンドを表示します

---

## 技術スタック

**バックエンド**
- Java 21 / Spring Boot 3.3.x
- PostgreSQL 15
- Gradle (Kotlin DSL)
- Resilience4j (Circuit Breaker / Retry / TimeLimiter / Bulkhead)
- Flyway / Testcontainers

**フロントエンド**
- React 18 + TypeScript / Vite
- TanStack Query v5
- shadcn/ui + Tailwind CSS
- React Router v6

---

## ローカル起動手順

### 前提条件

- Java 21
- Docker（PostgreSQL用）
- Last.fm API キー（https://www.last.fm/api/account/create で取得）

### 1. PostgreSQLを起動

```bash
docker run -d \
  --name metal-pg \
  -e POSTGRES_DB=metalexplorer \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15
```

### 2. 環境変数を設定

```bash
export DB_URL=jdbc:postgresql://localhost:5432/metalexplorer
export DB_USER=postgres
export DB_PASS=postgres
export LASTFM_API_KEY=your_api_key_here
```

### 3. アプリ起動

```bash
./gradlew bootRun
```

起動後、Flyway が自動でスキーマを作成します。

### 4. 動作確認

```bash
# バンド検索
curl "http://localhost:8080/api/bands/search?q=Metallica"

# ヘルスチェック
curl "http://localhost:8080/actuator/health"

# メトリクス
curl "http://localhost:8080/actuator/metrics"
```

### 5. テスト実行

Docker が起動している状態で実行してください（Testcontainers が PostgreSQL コンテナを自動起動します）。

```bash
./gradlew test
```

---

## フロントエンド起動手順

### 前提条件

- Node.js 18 以上

### 1. バックエンドを先に起動

上記「ローカル起動手順」の手順 1〜3 を実行してバックエンドを `localhost:8080` で起動してください。

### 2. 依存パッケージをインストール

```bash
cd frontend
npm install
```

### 3. 環境変数を設定（初回のみ）

```bash
cp .env.local.example .env.local
# 開発時は VITE_API_BASE_URL を空のままで可（Vite proxy が転送）
```

### 4. dev サーバを起動

```bash
npm run dev
# → http://localhost:5173
```

Vite の dev proxy により `localhost:5173/api/*` のリクエストは自動的に `localhost:8080` に転送されます。

### 5. フロントエンドのテスト実行

```bash
npm test
```

---

## APIエンドポイント

| Method | Path | 説明 |
|--------|------|------|
| GET | `/api/bands/search?q={query}` | バンド名で検索 |
| GET | `/api/bands/{artistId}/tags` | アーティストのタグ一覧 |
| GET | `/api/bands/{artistId}/similar` | 類似アーティスト一覧 |
| GET | `/actuator/health` | ヘルスチェック |
| GET | `/actuator/metrics` | メトリクス |
| GET | `/actuator/prometheus` | Prometheusメトリクス |

---

## インフラ (AWS)

本番環境は Terraform で管理する AWS 構成です。詳細は `infra/` ディレクトリを参照してください。

### 構成概要

| リソース | 詳細 |
|----------|------|
| コンピューティング | ECS Fargate (512 CPU / 1024 MB) |
| データベース | Aurora Serverless v2 PostgreSQL 16 (0.5〜4 ACU) |
| ロードバランサー | ALB (HTTPS 443 / HTTP→HTTPS redirect / test listener 8080) |
| デプロイ | CodeDeploy Blue/Green |
| コンテナレジストリ | ECR (IMMUTABLE タグ) |
| シークレット管理 | AWS Secrets Manager (DB認証情報 + Last.fm APIキー) |
| DNS / TLS | Cloudflare DNS + ACM 証明書 |
| ネットワーク | VPC (パブリック × 2 / プライベート × 2) + NAT Gateway (1 AZ) |
| VPC Endpoints | ECR API/DKR, S3 (Gateway), Secrets Manager, CloudWatch Logs |
| 監査ログ | CloudTrail |
| CI/CD | GitHub Actions (OIDC 認証) |

### Terraform 実行手順

```bash
cd infra

# 初期化
terraform init

# 確認
terraform plan

# 適用（初回は約15分）
terraform apply
```

**初回 apply 前に必要な設定:**

1. `terraform.tfvars` に実際の値を設定（`.gitignore` 済みのため Git には含まれない）:
   ```hcl
   cloudflare_api_token = "your_cloudflare_token"
   lastfm_api_key       = "your_lastfm_api_key"
   ```

2. GitHub OIDC プロバイダーが AWS アカウントに存在しない場合は `infra/github_actions.tf` のコメントアウトを解除して作成する

3. `terraform apply` 完了後、出力された `github_actions_role_arn` を GitHub リポジトリの `Settings → Secrets → AWS_ROLE_ARN` に設定する

### Docker イメージのビルド

```bash
# ローカルでビルド確認
docker build -t metal-band-explorer:local .

# 起動確認
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/metalexplorer \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  -e LASTFM_API_KEY=your_api_key \
  metal-band-explorer:local
```

---

## アーキテクチャ

詳細は [`docs/architecture.md`](docs/architecture.md) を参照してください（Mermaid 図: システム全体 / レイヤー構成 / Resilience4j フロー / ER 図 / カスタムメトリクス一覧）。

### 概要

```
BandController
    └─ ArtistService
           ├─ ArtistRepository (DB優先キャッシュ)
           └─ LastfmClient (未キャッシュ時のフォールバック先)
                  └─ Last.fm API (CircuitBreaker / Retry / TimeLimiter)
```

- DB にキャッシュがある場合は Last.fm API を呼ばない
- Circuit Breaker が Open の場合はDBキャッシュのみ返す
- 全リクエストは `search_history` に記録される

```
React UI (localhost:5173)
    └─ Vite dev proxy
           └─ BandController (localhost:8080)
                  └─ ArtistService
                         ├─ ArtistRepository (DB優先キャッシュ)
                         └─ LastfmClient (CircuitBreaker / Retry / TimeLimiter)
```
