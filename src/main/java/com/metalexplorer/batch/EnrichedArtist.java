package com.metalexplorer.batch;

import java.util.List;

public record EnrichedArtist(
        String name,
        String mbid,
        Long listeners,
        Long playcount,
        String bioSummary,
        List<Tag> tags,
        List<SimilarRef> similar
) {
    public record Tag(String name, int weight) {}
    public record SimilarRef(String name, String mbid, double score) {}
}
