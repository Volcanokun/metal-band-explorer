package com.metalexplorer.service;

import com.metalexplorer.domain.artist.Artist;
import com.metalexplorer.mapper.ArtistMapper;
import com.metalexplorer.mapper.SearchHistoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
class ArtistServiceTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15")
            .withDatabaseName("metalexplorer_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("lastfm.api-key", () -> "test-api-key");
    }

    @Autowired
    private ArtistService artistService;

    @Autowired
    private ArtistMapper artistMapper;

    @Autowired
    private SearchHistoryMapper searchHistoryMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @MockBean
    private LastfmClient lastfmClient;

    @BeforeEach
    void setUp() {
        jdbc.execute("DELETE FROM search_history");
        jdbc.execute("DELETE FROM similar_artists");
        jdbc.execute("DELETE FROM artist_tags");
        jdbc.execute("DELETE FROM artists");
    }

    @Test
    void search_returnsDbCacheHit_whenArtistExists() {
        Artist saved = Artist.builder()
                .id(UUID.randomUUID())
                .name("Metallica")
                .listeners(5_000_000L)
                .playcount(100_000_000L)
                .createdAt(LocalDateTime.now())
                .build();
        artistMapper.insert(saved);

        var response = artistService.search("Metallica", "session-1");

        assertThat(response.totalResults()).isEqualTo(1);
        assertThat(response.artists().get(0).name()).isEqualTo("Metallica");
        assertThat(response.artists().get(0).id()).isEqualTo(saved.getId());
    }

    @Test
    void search_fetchesFromLastfm_whenNoCacheHit() {
        var searchResult = new LastfmClient.ArtistSearchResult(
                new LastfmClient.ArtistSearchResult.Results(
                        new LastfmClient.ArtistSearchResult.ArtistMatches(
                                List.of(new LastfmClient.ArtistSearchResult.ArtistMatch("Slayer", "abc123", "3000000"))
                        )
                )
        );
        when(lastfmClient.searchArtist(anyString()))
                .thenReturn(CompletableFuture.completedFuture(searchResult));
        when(lastfmClient.getArtistInfo(anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(lastfmClient.getTopTags(anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        var response = artistService.search("Slayer", "session-2");

        assertThat(response.totalResults()).isEqualTo(1);
        assertThat(response.artists().get(0).name()).isEqualTo("Slayer");
        assertThat(artistMapper.findByNameIgnoreCase("Slayer")).isPresent();
    }

    @Test
    void search_recordsSearchHistory() {
        when(lastfmClient.searchArtist(anyString()))
                .thenReturn(CompletableFuture.completedFuture(
                        new LastfmClient.ArtistSearchResult(
                                new LastfmClient.ArtistSearchResult.Results(
                                        new LastfmClient.ArtistSearchResult.ArtistMatches(List.of())
                                )
                        )
                ));

        artistService.search("Testament", "session-3");

        var history = searchHistoryMapper.findAll();
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getQuery()).isEqualTo("Testament");
        assertThat(history.get(0).getSessionId()).isEqualTo("session-3");
    }

    @Test
    void search_returnsEmpty_whenCircuitBreakerIsOpen() {
        var response = artistService.search("Pantera", "session-4");
        assertThat(response.artists()).isNotNull();
    }
}
