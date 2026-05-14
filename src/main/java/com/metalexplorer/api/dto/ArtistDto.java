package com.metalexplorer.api.dto;

import java.util.List;
import java.util.UUID;

public record ArtistDto(
        UUID id,
        String name,
        String mbid,
        Long listeners,
        Long playcount,
        String bioSummary,
        List<TagDto> tags
) {}
