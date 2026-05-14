package com.metalexplorer.domain.artist;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "similar_artists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimilarArtist {

    @EmbeddedId
    private SimilarArtistId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("artistId")
    @JoinColumn(name = "artist_id")
    private Artist artist;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("similarArtistId")
    @JoinColumn(name = "similar_artist_id")
    private Artist similarArtistRef;

    private Double lastfmScore;
    private Double computedScore;

    @Column(length = 20)
    private String source;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class SimilarArtistId implements java.io.Serializable {
        private UUID artistId;
        private UUID similarArtistId;
    }
}
