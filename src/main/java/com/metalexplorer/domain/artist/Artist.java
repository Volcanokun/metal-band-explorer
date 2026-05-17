package com.metalexplorer.domain.artist;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Artist {

    private UUID id;
    private String name;
    private String mbid;
    private Long listeners;
    private Long playcount;
    private String bioSummary;
    private LocalDateTime lastFetchedAt;
    private LocalDateTime createdAt;
}
