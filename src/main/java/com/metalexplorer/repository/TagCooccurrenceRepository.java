package com.metalexplorer.repository;

import com.metalexplorer.domain.tag.TagCooccurrence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagCooccurrenceRepository extends JpaRepository<TagCooccurrence, TagCooccurrence.TagCooccurrenceId> {
}
