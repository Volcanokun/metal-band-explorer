CREATE TABLE IF NOT EXISTS artists (
    id              UUID         PRIMARY KEY,
    name            VARCHAR(255) NOT NULL UNIQUE,
    mbid            VARCHAR(36),
    listeners       BIGINT,
    playcount       BIGINT,
    bio_summary     TEXT,
    last_fetched_at TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_artists_name ON artists (name);

CREATE TABLE IF NOT EXISTS artist_tags (
    artist_id UUID         NOT NULL REFERENCES artists(id) ON DELETE CASCADE,
    tag_name  VARCHAR(100) NOT NULL,
    weight    INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (artist_id, tag_name)
);

CREATE TABLE IF NOT EXISTS similar_artists (
    artist_id         UUID   NOT NULL REFERENCES artists(id) ON DELETE CASCADE,
    similar_artist_id UUID   NOT NULL REFERENCES artists(id) ON DELETE CASCADE,
    lastfm_score      FLOAT,
    computed_score    FLOAT,
    source            VARCHAR(20),
    PRIMARY KEY (artist_id, similar_artist_id)
);

CREATE TABLE IF NOT EXISTS tag_cooccurrence (
    tag_a  VARCHAR(100) NOT NULL,
    tag_b  VARCHAR(100) NOT NULL,
    count  INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (tag_a, tag_b)
);

CREATE TABLE IF NOT EXISTS search_history (
    id                 UUID         PRIMARY KEY,
    session_id         VARCHAR(100) NOT NULL,
    query              VARCHAR(255) NOT NULL,
    resolved_artist_id UUID REFERENCES artists(id) ON DELETE SET NULL,
    searched_at        TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_search_history_session ON search_history (session_id);
