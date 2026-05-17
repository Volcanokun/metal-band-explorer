package com.metalexplorer.mapper;

import com.metalexplorer.domain.artist.SimilarArtist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface SimilarArtistMapper {

    List<SimilarArtist> findByArtistIdWithRef(@Param("artistId") UUID artistId);

    void deleteByArtistId(@Param("artistId") UUID artistId);

    void batchInsert(@Param("list") List<SimilarArtist> rows);

    int updateComputedScores();
}
