package com.metalexplorer.api.dto;

import java.util.UUID;

public record SimilarArtistDto(
        UUID id,
        String name,
        Double lastfmScore,
        Double computedScore
) {}
