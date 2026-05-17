package com.metalexplorer.domain.artist;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimilarArtist {

    private UUID artistId;
    private UUID similarArtistId;
    private String similarArtistName;
    private Double lastfmScore;
    private Double computedScore;
    private String source;
}
