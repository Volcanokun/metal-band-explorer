package com.metalexplorer.repository;

import com.metalexplorer.domain.artist.SimilarArtist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface SimilarArtistRepository extends JpaRepository<SimilarArtist, SimilarArtist.SimilarArtistId> {
    @Query("SELECT sa FROM SimilarArtist sa JOIN FETCH sa.similarArtistRef WHERE sa.id.artistId = :artistId ORDER BY sa.lastfmScore DESC")
    List<SimilarArtist> findByArtistIdWithRef(UUID artistId);
}
