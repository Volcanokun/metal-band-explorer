package com.metalexplorer.batch.steps;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@StepScope
@Slf4j
public class SeedArtistReader implements ItemReader<String> {

    private final JdbcTemplate jdbc;
    private Iterator<String> iterator;

    public SeedArtistReader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String read() {
        if (iterator == null) {
            initialize();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }

    private void initialize() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM artists", Long.class);
        if (count == null || count == 0) {
            log.info("Artists table is empty — loading from seeds.csv");
            iterator = loadFromCsv().iterator();
        } else {
            log.info("Loading stale artists from DB (last_fetched_at older than 7 days)");
            iterator = loadStaleFromDb().iterator();
        }
    }

    private List<String> loadFromCsv() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource("seeds.csv").getInputStream()))) {
            return reader.lines()
                    .skip(1)
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read seeds.csv", e);
        }
    }

    private List<String> loadStaleFromDb() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        return jdbc.queryForList(
                """
                SELECT name FROM artists
                WHERE last_fetched_at IS NULL
                   OR last_fetched_at < ?
                ORDER BY last_fetched_at ASC NULLS FIRST
                LIMIT 500
                """,
                String.class, cutoff);
    }
}
