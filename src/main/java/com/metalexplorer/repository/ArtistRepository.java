package com.metalexplorer.repository;

import com.metalexplorer.domain.artist.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArtistRepository extends JpaRepository<Artist, UUID> {
    List<Artist> findByNameContainingIgnoreCase(String name);
    Optional<Artist> findByNameIgnoreCase(String name);
}
