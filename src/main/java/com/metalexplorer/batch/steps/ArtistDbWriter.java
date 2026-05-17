package com.metalexplorer.batch.steps;

import com.metalexplorer.batch.EnrichedArtist;
import com.metalexplorer.domain.artist.Artist;
import com.metalexplorer.domain.artist.ArtistTag;
import com.metalexplorer.domain.artist.SimilarArtist;
import com.metalexplorer.mapper.ArtistMapper;
import com.metalexplorer.mapper.ArtistTagMapper;
import com.metalexplorer.mapper.SimilarArtistMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class ArtistDbWriter implements ItemWriter<EnrichedArtist> {

    private final ArtistMapper artistMapper;
    private final ArtistTagMapper artistTagMapper;
    private final SimilarArtistMapper similarArtistMapper;

    @Override
    @Transactional
    public void write(Chunk<? extends EnrichedArtist> chunk) {
        for (EnrichedArtist enriched : chunk.getItems()) {
            Artist artist = upsertArtist(enriched);
            replaceTags(artist.getId(), enriched.tags());
            replaceSimilarArtists(artist.getId(), enriched.similar());
            log.debug("Wrote artist '{}' with {} tags, {} similar", artist.getName(),
                    enriched.tags().size(), enriched.similar().size());
        }
    }

    private Artist upsertArtist(EnrichedArtist enriched) {
        return artistMapper.findByNameIgnoreCase(enriched.name())
                .map(existing -> {
                    existing.setMbid(enriched.mbid());
                    existing.setListeners(enriched.listeners());
                    existing.setPlaycount(enriched.playcount());
                    existing.setBioSummary(enriched.bioSummary());
                    existing.setLastFetchedAt(LocalDateTime.now());
                    artistMapper.update(existing);
                    return existing;
                })
                .orElseGet(() -> {
                    Artist artist = Artist.builder()
                            .id(UUID.randomUUID())
                            .name(enriched.name())
                            .mbid(enriched.mbid())
                            .listeners(enriched.listeners())
                            .playcount(enriched.playcount())
                            .bioSummary(enriched.bioSummary())
                            .lastFetchedAt(LocalDateTime.now())
                            .createdAt(LocalDateTime.now())
                            .build();
                    artistMapper.insert(artist);
                    return artist;
                });
    }

    private void replaceTags(UUID artistId, List<EnrichedArtist.Tag> tags) {
        artistTagMapper.deleteByArtistId(artistId);
        if (tags.isEmpty()) return;

        List<ArtistTag> list = tags.stream()
                .map(t -> ArtistTag.builder()
                        .artistId(artistId)
                        .tagName(t.name())
                        .weight(t.weight())
                        .build())
                .toList();
        artistTagMapper.batchInsert(list);
    }

    private void replaceSimilarArtists(UUID artistId, List<EnrichedArtist.SimilarRef> similar) {
        similarArtistMapper.deleteByArtistId(artistId);
        if (similar.isEmpty()) return;

        List<SimilarArtist> rows = similar.stream()
                .map(ref -> {
                    UUID simId = resolveArtistId(ref.name(), ref.mbid());
                    return SimilarArtist.builder()
                            .artistId(artistId)
                            .similarArtistId(simId)
                            .lastfmScore(ref.score())
                            .source("lastfm")
                            .build();
                })
                .filter(sa -> !sa.getSimilarArtistId().equals(artistId))
                .toList();

        if (!rows.isEmpty()) {
            similarArtistMapper.batchInsert(rows);
        }
    }

    private UUID resolveArtistId(String name, String mbid) {
        return artistMapper.findByNameIgnoreCase(name)
                .map(Artist::getId)
                .orElseGet(() -> {
                    Artist stub = Artist.builder()
                            .id(UUID.randomUUID())
                            .name(name)
                            .mbid(mbid)
                            .createdAt(LocalDateTime.now())
                            .build();
                    artistMapper.insert(stub);
                    return stub.getId();
                });
    }
}
