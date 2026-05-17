package com.metalexplorer.resilience;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.metalexplorer.domain.artist.Artist;
import com.metalexplorer.mapper.ArtistMapper;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.UUID;
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
        "resilience4j.retry.instances.lastfmApi.max-attempts=1",
        "resilience4j.circuitbreaker.instances.lastfmApi.wait-duration-in-open-state=2s",
        "lastfm.api-key=test-key",
})
class ResilienceIntegrationTest {

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
    @Autowired ArtistMapper artistMapper;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        jdbc.execute("DELETE FROM search_history");
        jdbc.execute("DELETE FROM similar_artists");
        jdbc.execute("DELETE FROM artist_tags");
        jdbc.execute("DELETE FROM artists");
        circuitBreakerRegistry.circuitBreaker("lastfmApi").reset();
    }

    @Test
    void circuitBreaker_opensAfter10Failures() {
        wireMock.stubFor(any(anyUrl())
                .willReturn(aResponse().withStatus(500)));

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("lastfmApi");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        for (int i = 0; i < 10; i++) {
            try {
                lastfmClient.searchArtist("unique-query-" + i).get();
            } catch (Exception ignored) {}
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void fallback_returnsDbCache_whenCircuitBreakerIsOpen() {
        wireMock.stubFor(any(anyUrl())
                .willReturn(aResponse().withStatus(500)));

        artistMapper.insert(Artist.builder()
                .id(UUID.randomUUID())
                .name("Metallica")
                .createdAt(LocalDateTime.now())
                .build());

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("lastfmApi");
        cb.transitionToOpenState();

        var response = artistService.search("Metallica", "session-fallback");

        assertThat(response.artists()).hasSize(1);
        assertThat(response.artists().get(0).name()).isEqualTo("Metallica");
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void timeLimiter_triggersOn6SecondDelay() {
        wireMock.stubFor(any(anyUrl())
                .willReturn(aResponse()
                        .withFixedDelay(6_000)
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        var future = lastfmClient.searchArtist("slow-metal-band");

        assertThatThrownBy(() -> future.get(10, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(TimeoutException.class);
    }
}
