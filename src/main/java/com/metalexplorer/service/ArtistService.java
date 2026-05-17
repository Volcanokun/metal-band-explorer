package com.metalexplorer.service;

import com.metalexplorer.api.dto.ArtistDto;
import com.metalexplorer.api.dto.SearchResponse;
import com.metalexplorer.api.dto.SimilarArtistDto;
import com.metalexplorer.api.dto.TagDto;
import com.metalexplorer.domain.artist.Artist;
import com.metalexplorer.domain.artist.ArtistTag;
import com.metalexplorer.domain.history.SearchHistory;
import com.metalexplorer.mapper.ArtistMapper;
import com.metalexplorer.mapper.ArtistTagMapper;
import com.metalexplorer.mapper.SearchHistoryMapper;
import com.metalexplorer.mapper.SimilarArtistMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtistService {

    private final ArtistMapper artistMapper;
    private final ArtistTagMapper artistTagMapper;
    private final SimilarArtistMapper similarArtistMapper;
    private final SearchHistoryMapper searchHistoryMapper;
    private final LastfmClient lastfmClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final MetricsService metricsService;

    @Transactional
    public SearchResponse search(String query, String sessionId) {
        MDC.put("artistName", query);
        try {
            List<Artist> dbResults = artistMapper.findByNameContainingIgnoreCase(query);
            if (!dbResults.isEmpty()) {
                log.info("DB cache hit for query: {}", query);
                metricsService.recordCacheHit("db");
                recordHistory(sessionId, query, dbResults.get(0).getId());
                return toSearchResponse(query, dbResults);
            }

            List<Artist> fetched = fetchAndCacheFromLastfm(query);
            UUID resolvedId = fetched.isEmpty() ? null : fetched.get(0).getId();
            recordHistory(sessionId, query, resolvedId);
            return toSearchResponse(query, fetched);
        } finally {
            MDC.remove("artistName");
        }
    }

    @Transactional(readOnly = true)
    public List<TagDto> getTagsByArtistId(UUID artistId) {
        return artistTagMapper.findByArtistIdOrderByWeightDesc(artistId)
                .stream()
                .map(t -> new TagDto(t.getTagName(), t.getWeight()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SimilarArtistDto> getSimilarArtists(UUID artistId) {
        return similarArtistMapper.findByArtistIdWithRef(artistId)
                .stream()
                .map(sa -> new SimilarArtistDto(
                        sa.getSimilarArtistId(),
                        sa.getSimilarArtistName(),
                        sa.getLastfmScore(),
                        sa.getComputedScore()))
                .toList();
    }

    private List<Artist> fetchAndCacheFromLastfm(String query) {
        var sample = metricsService.startLatencySample();
        try {
            var searchResult = lastfmClient.searchArtist(query).get();
            metricsService.stopLatencySample(sample, "searchArtist");
            metricsService.recordApiCall("searchArtist", true);

            if (searchResult == null || searchResult.results() == null) return List.of();
            var matches = searchResult.results().artistmatches().artist();
            if (matches == null || matches.isEmpty()) return List.of();

            metricsService.recordCacheHit("api");
            return matches.stream()
                    .map(match -> fetchAndSaveArtistDetails(match.name(), match.mbid()))
                    .toList();
        } catch (Exception e) {
            metricsService.stopLatencySample(sample, "searchArtist");
            metricsService.recordApiCall("searchArtist", false);
            return lastfmClientSearchFallback(query, e);
        }
    }

    private List<Artist> lastfmClientSearchFallback(String query, Exception ex) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("lastfmApi");
        log.warn("Last.fm API fallback triggered",
                kv("cbState", cb.getState().name()),
                kv("exceptionType", ex.getClass().getSimpleName()),
                kv("query", query));
        return artistMapper.findByNameContainingIgnoreCase(query);
    }

    private Artist fetchAndSaveArtistDetails(String name, String mbid) {
        Optional<Artist> existing = artistMapper.findByNameIgnoreCase(name);
        if (existing.isPresent()) return existing.get();

        Artist artist = Artist.builder()
                .id(UUID.randomUUID())
                .name(name)
                .mbid(mbid != null && !mbid.isBlank() ? mbid : null)
                .lastFetchedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        try {
            var info = lastfmClient.getArtistInfo(name).get();
            if (info != null && info.artist() != null) {
                var ai = info.artist();
                if (ai.stats() != null) {
                    artist.setListeners(parseLong(ai.stats().listeners()));
                    artist.setPlaycount(parseLong(ai.stats().playcount()));
                }
                if (ai.bio() != null) {
                    artist.setBioSummary(ai.bio().summary());
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch artist info for {}: {}", name, e.getMessage());
        }

        artistMapper.insert(artist);

        try {
            var tagsResult = lastfmClient.getTopTags(name).get();
            if (tagsResult != null && tagsResult.toptags() != null) {
                var tagList = tagsResult.toptags().tag();
                if (tagList != null && !tagList.isEmpty()) {
                    List<ArtistTag> artistTags = tagList.stream().limit(10)
                            .map(t -> ArtistTag.builder()
                                    .artistId(artist.getId())
                                    .tagName(t.name())
                                    .weight(parseInt(t.count()))
                                    .build())
                            .toList();
                    artistTagMapper.batchInsert(artistTags);
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch tags for {}: {}", name, e.getMessage());
        }

        return artist;
    }

    private void recordHistory(String sessionId, String query, UUID resolvedArtistId) {
        searchHistoryMapper.insert(SearchHistory.builder()
                .id(UUID.randomUUID())
                .sessionId(sessionId)
                .query(query)
                .resolvedArtistId(resolvedArtistId)
                .searchedAt(LocalDateTime.now())
                .build());
    }

    private SearchResponse toSearchResponse(String query, List<Artist> artists) {
        var dtos = artists.stream().map(a -> new ArtistDto(
                a.getId(), a.getName(), a.getMbid(),
                a.getListeners(), a.getPlaycount(), a.getBioSummary(),
                artistTagMapper.findByArtistIdOrderByWeightDesc(a.getId())
                        .stream().map(t -> new TagDto(t.getTagName(), t.getWeight())).toList()
        )).toList();
        return new SearchResponse(query, dtos.size(), dtos);
    }

    private static Long parseLong(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Long.parseLong(s.replaceAll(",", "")); } catch (NumberFormatException e) { return null; }
    }

    private static int parseInt(String s) {
        if (s == null || s.isBlank()) return 0;
        try { return Integer.parseInt(s.replaceAll(",", "")); } catch (NumberFormatException e) { return 0; }
    }
}
