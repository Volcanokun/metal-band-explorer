# metal-band-explorer

メタルにはサブジャンルがあまりにも多すぎるため、好きなメタルバンドを入力すると類似のバンドを表示します

---

## 技術スタック

- Java 21 / Spring Boot 3.3.x
- PostgreSQL 15
- Gradle (Kotlin DSL)
- Resilience4j (Circuit Breaker / Retry / TimeLimiter / Bulkhead)
- Flyway
- Testcontainers

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

## アーキテクチャ概要

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
