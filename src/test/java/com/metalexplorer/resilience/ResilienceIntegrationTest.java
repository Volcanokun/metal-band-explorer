package com.metalexplorer.resilience;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.metalexplorer.domain.artist.Artist;
import com.metalexplorer.repository.ArtistRepository;
import com.metalexplorer.repository.SearchHistoryRepository;
import com.metalexplorer.service.ArtistService;
import com.metalexplorer.service.LastfmClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@TestPropertySource(properties = {
        // 各テストが独立して CB を評価できるよう retry を 1 回に絞る
        "resilience4j.retry.instances.lastfmApi.max-attempts=1",
        // テストが終わるまで待たなくて済むよう open → half-open 復帰を短縮
        "resilience4j.circuitbreaker.instances.lastfmApi.wait-duration-in-open-state=2s",
        "lastfm.api-key=test-key",
})
class ResilienceIntegrationTest {

    // Spring context 生成より前に起動する必要があるため static イニシャライザで開始
    static final WireMockServer wireMock;

    static {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
    }

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15")
            .withDatabaseName("metalexplorer_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("lastfm.base-url", () -> "http://localhost:" + wireMock.port());
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @Autowired ArtistService artistService;
    @Autowired LastfmClient lastfmClient;
    @Autowired CircuitBreakerRegistry circuitBreakerRegistry;
    @Autowired ArtistRepository artistRepository;
    @Autowired SearchHistoryRepository searchHistoryRepository;

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        searchHistoryRepository.deleteAll();
        artistRepository.deleteAll();
        // 各テスト開始時に CB を CLOSED に戻す
        circuitBreakerRegistry.circuitBreaker("lastfmApi").reset();
    }

    // ------------------------------------------------------------------
    // テスト 1: 10 回連続失敗で CB が OPEN になること
    // ------------------------------------------------------------------
    @Test
    void circuitBreaker_opensAfter10Failures() {
        wireMock.stubFor(any(anyUrl())
                .willReturn(aResponse().withStatus(500)));

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("lastfmApi");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // DB にキャッシュが無い 10 クエリを投げ、すべて API 失敗 → CB がカウントアップ
        for (int i = 0; i < 10; i++) {
            artistService.search("unique-query-" + i, "session-cb-" + i);
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    // ------------------------------------------------------------------
    // テスト 2: OPEN 状態でリクエストするとフォールバックが DB キャッシュを返すこと
    // ------------------------------------------------------------------
    @Test
    void fallback_returnsDbCache_whenCircuitBreakerIsOpen() {
        // API は何も返さない（CB が OPEN なので実際には呼ばれないが stub しておく）
        wireMock.stubFor(any(anyUrl())
                .willReturn(aResponse().withStatus(500)));

        // あらかじめ DB に Metallica を保存
        artistRepository.save(Artist.builder().name("Metallica").build());

        // CB を強制 OPEN に遷移
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("lastfmApi");
        cb.transitionToOpenState();

        // DB キャッシュが返るはずなので結果が 1 件あること
        var response = artistService.search("Metallica", "session-fallback");

        assertThat(response.artists()).hasSize(1);
        assertThat(response.artists().get(0).name()).isEqualTo("Metallica");
    }

    // ------------------------------------------------------------------
    // テスト 3: 6 秒遅延で TimeLimiter (5 秒タイムアウト) が発動すること
    // ------------------------------------------------------------------
    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void timeLimiter_triggersOn6SecondDelay() {
        wireMock.stubFor(any(anyUrl())
                .willReturn(aResponse()
                        .withFixedDelay(6_000)
                        .withStatus(200)
                        .withBody("{}")));

        // AOP プロキシ経由で @TimeLimiter が適用された CompletableFuture を取得
        var future = lastfmClient.searchArtist("slow-metal-band");

        // 5 秒で TimeLimiter が発動 → ExecutionException(cause: TimeoutException)
        assertThatThrownBy(() -> future.get(10, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(TimeoutException.class);
    }
}
