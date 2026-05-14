package com.metalexplorer.api.dto;

import java.util.List;

public record SearchResponse(
        String query,
        int totalResults,
        List<ArtistDto> artists
) {}
