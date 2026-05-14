package com.metalexplorer.repository;

import com.metalexplorer.domain.history.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, UUID> {
}
