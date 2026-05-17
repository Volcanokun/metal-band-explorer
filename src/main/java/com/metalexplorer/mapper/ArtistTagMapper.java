package com.metalexplorer.mapper;

import com.metalexplorer.domain.artist.ArtistTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface ArtistTagMapper {

    List<ArtistTag> findByArtistIdOrderByWeightDesc(@Param("artistId") UUID artistId);

    void deleteByArtistId(@Param("artistId") UUID artistId);

    void batchInsert(@Param("list") List<ArtistTag> tags);
}
