package com.metalexplorer.domain.history;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchHistory {

    private UUID id;
    private String sessionId;
    private String query;
    private UUID resolvedArtistId;
    private LocalDateTime searchedAt;
}
