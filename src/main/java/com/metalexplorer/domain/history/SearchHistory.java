package com.metalexplorer.domain.history;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "search_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Column(nullable = false, length = 255)
    private String query;

    @Column(name = "resolved_artist_id")
    private UUID resolvedArtistId;

    @Column(name = "searched_at", nullable = false)
    private LocalDateTime searchedAt;

    @PrePersist
    void prePersist() {
        searchedAt = LocalDateTime.now();
    }
}
