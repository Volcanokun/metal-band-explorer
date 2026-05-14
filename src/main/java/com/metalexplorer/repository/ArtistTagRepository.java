package com.metalexplorer.repository;

import com.metalexplorer.domain.artist.ArtistTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ArtistTagRepository extends JpaRepository<ArtistTag, ArtistTag.ArtistTagId> {
    List<ArtistTag> findByIdArtistIdOrderByWeightDesc(UUID artistId);
}
