package com.metalexplorer.batch.steps;

import com.metalexplorer.batch.EnrichedArtist;
import com.metalexplorer.service.LastfmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletionException;

@Component
@Slf4j
@RequiredArgsConstructor
public class LastfmEnrichProcessor implements ItemProcessor<String, EnrichedArtist> {

    private final LastfmClient client;

    @Override
    public EnrichedArtist process(String name) throws Exception {
        log.debug("Enriching artist: {}", name);
        try {
            var infoResult   = client.getArtistInfo(name).join();
            var tagsResult   = client.getTopTags(name).join();
            var similarResult = client.getSimilarArtists(name).join();

            if (infoResult == null || infoResult.artist() == null) {
                log.warn("Artist not found on Last.fm, skipping: {}", name);
                return null;
            }

            var info = infoResult.artist();
            return new EnrichedArtist(
                    info.name() != null ? info.name() : name,
                    info.mbid(),
                    parseLong(info.stats() != null ? info.stats().listeners() : null),
                    parseLong(info.stats() != null ? info.stats().playcount() : null),
                    info.bio() != null ? info.bio().summary() : null,
                    tagsResult != null && tagsResult.toptags() != null && tagsResult.toptags().tag() != null
                            ? tagsResult.toptags().tag().stream()
                                .map(t -> new EnrichedArtist.Tag(t.name(), parseInt(t.count())))
                                .toList()
                            : List.of(),
                    similarResult != null && similarResult.similarartists() != null && similarResult.similarartists().artist() != null
                            ? similarResult.similarartists().artist().stream()
                                .map(s -> new EnrichedArtist.SimilarRef(s.name(), s.mbid(), parseDouble(s.match())))
                                .toList()
                            : List.of()
            );
        } catch (CompletionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.warn("Failed to enrich artist '{}': {} — will skip", name, cause.getMessage());
            throw (Exception) cause;
        }
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Long.parseLong(value.replace(",", "")); } catch (NumberFormatException e) { return null; }
    }

    private static int parseInt(String value) {
        if (value == null || value.isBlank()) return 0;
        try { return Integer.parseInt(value.replace(",", "")); } catch (NumberFormatException e) { return 0; }
    }

    private static double parseDouble(String value) {
        if (value == null || value.isBlank()) return 0.0;
        try { return Double.parseDouble(value); } catch (NumberFormatException e) { return 0.0; }
    }
}
