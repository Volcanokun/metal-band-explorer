# アーキテクチャ概要

## システム全体図

```mermaid
graph TB
    User(["ユーザー"])

    subgraph FE["フロントエンド  (localhost:5173)"]
        direction TB
        SearchPage["SearchPage\n検索 + 履歴"]
        DetailPage["ArtistDetailPage\nタグ / 類似バンド"]
        TQ["TanStack Query v5\nキャッシュ・ローディング管理"]
        RR["React Router v6\nSPA ルーティング"]
        ShadCN["shadcn/ui + Tailwind CSS\nダークテーマ"]
    end

    subgraph Proxy["Vite Dev Proxy"]
        P["/api/* → :8080"]
    end

    subgraph BE["バックエンド  (localhost:8080)"]
        direction TB
        Controller["BandController\nREST API"]
        Service["ArtistService\nビジネスロジック"]
        Metrics["MetricsService\nMicrometer カスタムメトリクス"]
        CBListener["CircuitBreakerEventListener\n状態遷移ログ"]

        subgraph R4J["Resilience4j"]
            CB["CircuitBreaker\nsliding-window=10 / threshold=50%"]
            Retry["Retry\nmax=3 / exponential backoff"]
            TL["TimeLimiter\ntimeout=5s"]
            BH["Bulkhead\nmax-concurrent=10"]
        end

        Controller --> Service
        Service --> Metrics
        Service --> R4J
        CBListener --> Metrics
    end

    subgraph Infra["インフラ"]
        DB[("PostgreSQL 15\nartists / artist_tags\nsimilar_artists / search_history")]
        LASTFM["Last.fm API\n(外部)"]
        CW["Amazon CloudWatch\n(本番時)"]
        Flyway["Flyway\nスキーマ管理"]
    end

    User -->|"検索・閲覧"| FE
    FE --> Proxy
    Proxy -->|"HTTP"| Controller
    Service -->|"DB 優先キャッシュ\n(読み取り)"| DB
    Service -->|"キャッシュ書き込み"| DB
    R4J -->|"未キャッシュ時"| LASTFM
    Service -->|"fallback 時も DB 検索"| DB
    Metrics -->|"CLOUDWATCH_ENABLED=true 時"| CW
    Flyway -.->|"起動時マイグレーション"| DB
```

---

## バックエンド レイヤー構成

```mermaid
graph LR
    subgraph "api/"
        BC["BandController"]
        DTO["SearchResponse\nArtistDto / TagDto\nSimilarArtistDto"]
    end

    subgraph "service/"
        AS["ArtistService"]
        LC["LastfmClient\n@CircuitBreaker\n@Retry\n@TimeLimiter"]
        MS["MetricsService"]
    end

    subgraph "domain/"
        Artist["Artist"]
        ArtistTag["ArtistTag"]
        SimilarArtist["SimilarArtist"]
        SearchHistory["SearchHistory"]
    end

    subgraph "repository/"
        AR["ArtistRepository"]
        ATR["ArtistTagRepository"]
        SAR["SimilarArtistRepository"]
        SHR["SearchHistoryRepository"]
    end

    subgraph "infrastructure/config/"
        RC["ResilienceConfig"]
        LC2["LoggingConfig\nMDC traceId フィルター"]
        CBL["CircuitBreakerEventListenerConfig"]
    end

    BC --> AS
    AS --> LC
    AS --> MS
    AS --> AR
    AS --> ATR
    AS --> SAR
    AS --> SHR
    AR --> Artist
    ATR --> ArtistTag
    SAR --> SimilarArtist
    SHR --> SearchHistory
    CBL --> MS
```

---

## Resilience4j パターン（LastfmClient の呼び出しフロー）

```mermaid
sequenceDiagram
    participant S as ArtistService
    participant CB as CircuitBreaker
    participant TL as TimeLimiter
    participant RT as Retry
    participant API as Last.fm API
    participant DB as PostgreSQL

    S->>CB: searchArtist(query)
    alt CB = CLOSED / HALF_OPEN
        CB->>TL: allow (5s タイムアウト付き)
        TL->>RT: 非同期実行
        RT->>API: HTTP GET
        alt 成功
            API-->>S: ArtistSearchResult
            S->>DB: キャッシュ書き込み
        else HTTP エラー / タイムアウト (retry ≤ 3)
            RT->>API: リトライ (exponential backoff)
        else 失敗率 ≥ 50% (10件ウィンドウ)
            CB-->>CB: CLOSED → OPEN
        end
    else CB = OPEN
        CB-->>S: CallNotPermittedException
        S->>DB: fallback: DB キャッシュ検索
    end
```

---

## データモデル

```mermaid
erDiagram
    artists {
        uuid id PK
        varchar name
        varchar mbid
        bigint listeners
        bigint playcount
        text bio_summary
        timestamp last_fetched_at
    }
    artist_tags {
        uuid artist_id FK
        varchar tag_name
        int weight
    }
    similar_artists {
        uuid artist_id FK
        uuid similar_artist_id FK
        float lastfm_score
        float computed_score
    }
    search_history {
        uuid id PK
        varchar session_id
        varchar query
        uuid resolved_artist_id FK
        timestamp searched_at
    }

    artists ||--o{ artist_tags : "has"
    artists ||--o{ similar_artists : "has"
    artists ||--o{ search_history : "resolved to"
```

---

## カスタムメトリクス一覧

| メトリクス名 | 種別 | タグ | 説明 |
|---|---|---|---|
| `lastfm.api.call.total` | Counter | `method`, `status=success\|failure` | Last.fm API 呼び出し回数 |
| `lastfm.api.latency` | Timer (Histogram) | `method` | API レイテンシ分布 |
| `artist.cache.hit` | Counter | `source=db\|api` | キャッシュヒット元 |
| `circuitbreaker.state` | Gauge | `name=lastfmApi` | CB 状態 (0=CLOSED / 1=OPEN / 2=HALF_OPEN) |

CloudWatch カスタム名前空間: **`MetalExplorer`**（`CLOUDWATCH_ENABLED=true` で有効）
