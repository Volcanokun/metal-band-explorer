package com.metalexplorer.domain.artist;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "artist_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistTag {

    @EmbeddedId
    private ArtistTagId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("artistId")
    @JoinColumn(name = "artist_id")
    private Artist artist;

    @Column(name = "weight")
    private int weight;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class ArtistTagId implements java.io.Serializable {
        private UUID artistId;
        @Column(name = "tag_name")
        private String tagName;
    }
}
