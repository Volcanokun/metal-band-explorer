package com.metalexplorer.mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TagCooccurrenceMapper {

    void deleteAll();

    int insertFromArtistTags();
}
