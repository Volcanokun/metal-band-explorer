package com.metalexplorer.domain.artist;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistTag {

    private UUID artistId;
    private String tagName;
    private int weight;
}
