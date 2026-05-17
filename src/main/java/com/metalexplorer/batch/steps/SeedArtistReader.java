package com.metalexplorer.batch.steps;

import com.metalexplorer.mapper.ArtistMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.core.io.ClassPathResource;
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

    private final ArtistMapper artistMapper;
    private Iterator<String> iterator;

    public SeedArtistReader(ArtistMapper artistMapper) {
        this.artistMapper = artistMapper;
    }

    @Override
    public String read() {
        if (iterator == null) {
            initialize();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }

    private void initialize() {
        long count = artistMapper.count();
        if (count == 0) {
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
        return artistMapper.findStaleNames(cutoff, 500);
    }
}
