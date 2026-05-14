package com.metalexplorer.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class LastfmClient {

    private final RestClient restClient;
    private final String apiKey;

    public LastfmClient(RestClient.Builder builder,
                        @Value("${lastfm.base-url}") String baseUrl,
                        @Value("${lastfm.api-key}") String apiKey) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    @CircuitBreaker(name = "lastfmApi")
    @Retry(name = "lastfmApi")
    @TimeLimiter(name = "lastfmApi")
    public CompletableFuture<ArtistSearchResult> searchArtist(String query) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Calling Last.fm artist.search: {}", query);
            return restClient.get()
                    .uri(u -> u.queryParam("method", "artist.search")
                               .queryParam("artist", query)
                               .queryParam("api_key", apiKey)
                               .queryParam("format", "json")
                               .build())
                    .retrieve()
                    .body(ArtistSearchResult.class);
        });
    }

    @CircuitBreaker(name = "lastfmApi")
    @Retry(name = "lastfmApi")
    @TimeLimiter(name = "lastfmApi")
    public CompletableFuture<ArtistInfoResult> getArtistInfo(String artistName) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Calling Last.fm artist.getInfo: {}", artistName);
            return restClient.get()
                    .uri(u -> u.queryParam("method", "artist.getInfo")
                               .queryParam("artist", artistName)
                               .queryParam("api_key", apiKey)
                               .queryParam("format", "json")
                               .build())
                    .retrieve()
                    .body(ArtistInfoResult.class);
        });
    }

    @CircuitBreaker(name = "lastfmApi")
    @Retry(name = "lastfmApi")
    @TimeLimiter(name = "lastfmApi")
    public CompletableFuture<TopTagsResult> getTopTags(String artistName) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Calling Last.fm artist.getTopTags: {}", artistName);
            return restClient.get()
                    .uri(u -> u.queryParam("method", "artist.getTopTags")
                               .queryParam("artist", artistName)
                               .queryParam("api_key", apiKey)
                               .queryParam("format", "json")
                               .build())
                    .retrieve()
                    .body(TopTagsResult.class);
        });
    }

    @CircuitBreaker(name = "lastfmApi")
    @Retry(name = "lastfmApi")
    @TimeLimiter(name = "lastfmApi")
    public CompletableFuture<SimilarArtistsResult> getSimilarArtists(String artistName) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Calling Last.fm artist.getSimilar: {}", artistName);
            return restClient.get()
                    .uri(u -> u.queryParam("method", "artist.getSimilar")
                               .queryParam("artist", artistName)
                               .queryParam("limit", "10")
                               .queryParam("api_key", apiKey)
                               .queryParam("format", "json")
                               .build())
                    .retrieve()
                    .body(SimilarArtistsResult.class);
        });
    }

    // --- Response DTOs (Records) ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ArtistSearchResult(Results results) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Results(ArtistMatches artistmatches) {}
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record ArtistMatches(List<ArtistMatch> artist) {}
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record ArtistMatch(String name, String mbid, String listeners) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ArtistInfoResult(ArtistInfo artist) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record ArtistInfo(String name, String mbid, Stats stats, Bio bio) {}
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Stats(String listeners, String playcount) {}
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Bio(String summary) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TopTagsResult(TopTags toptags) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record TopTags(List<Tag> tag) {}
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Tag(String name, String count) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SimilarArtistsResult(Similarartists similarartists) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Similarartists(List<SimilarArtist> artist) {}
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record SimilarArtist(String name, String mbid, String match) {}
    }
}
