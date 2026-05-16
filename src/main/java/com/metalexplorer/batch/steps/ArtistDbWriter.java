package com.metalexplorer.batch.steps;

import com.metalexplorer.batch.EnrichedArtist;
import com.metalexplorer.domain.artist.Artist;
import com.metalexplorer.domain.artist.ArtistTag;
import com.metalexplorer.domain.artist.SimilarArtist;
import com.metalexplorer.repository.ArtistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class ArtistDbWriter implements ItemWriter<EnrichedArtist> {

    private final ArtistRepository artistRepository;
    private final JdbcTemplate jdbc;

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
        Artist artist = artistRepository.findByNameIgnoreCase(enriched.name())
                .orElseGet(() -> Artist.builder().name(enriched.name()).build());

        artist.setMbid(enriched.mbid());
        artist.setListeners(enriched.listeners());
        artist.setPlaycount(enriched.playcount());
        artist.setBioSummary(enriched.bioSummary());
        artist.setLastFetchedAt(LocalDateTime.now());

        return artistRepository.saveAndFlush(artist);
    }

    private void replaceTags(UUID artistId, List<EnrichedArtist.Tag> tags) {
        jdbc.update("DELETE FROM artist_tags WHERE artist_id = ?", artistId);
        if (tags.isEmpty()) return;

        List<Object[]> batch = tags.stream()
                .map(t -> new Object[]{artistId, t.name(), t.weight()})
                .toList();
        jdbc.batchUpdate(
                "INSERT INTO artist_tags (artist_id, tag_name, weight) VALUES (?, ?, ?)",
                batch);
    }

    private void replaceSimilarArtists(UUID artistId, List<EnrichedArtist.SimilarRef> similar) {
        jdbc.update("DELETE FROM similar_artists WHERE artist_id = ?", artistId);
        if (similar.isEmpty()) return;

        List<Object[]> batch = new ArrayList<>();
        for (EnrichedArtist.SimilarRef ref : similar) {
            UUID simId = resolveArtistId(ref.name(), ref.mbid());
            if (!simId.equals(artistId)) {
                batch.add(new Object[]{artistId, simId, ref.score(), "lastfm"});
            }
        }
        if (!batch.isEmpty()) {
            jdbc.batchUpdate(
                    "INSERT INTO similar_artists (artist_id, similar_artist_id, lastfm_score, source) VALUES (?, ?, ?, ?)",
                    batch);
        }
    }

    private UUID resolveArtistId(String name, String mbid) {
        return artistRepository.findByNameIgnoreCase(name)
                .map(Artist::getId)
                .orElseGet(() -> {
                    Artist stub = Artist.builder().name(name).mbid(mbid).build();
                    return artistRepository.saveAndFlush(stub).getId();
                });
    }
}
