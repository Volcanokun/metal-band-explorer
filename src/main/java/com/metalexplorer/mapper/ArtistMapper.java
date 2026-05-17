package com.metalexplorer.mapper;

import com.metalexplorer.domain.artist.Artist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface ArtistMapper {

    Optional<Artist> findByNameIgnoreCase(String name);

    List<Artist> findByNameContainingIgnoreCase(String name);

    void insert(Artist artist);

    void update(Artist artist);

    long count();

    List<String> findStaleNames(@Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);
}
