package com.metalexplorer.batch;

import com.metalexplorer.batch.steps.TagCooccurrenceStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("batch-test")
class TagCooccurrenceStepTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        jdbc.execute("DELETE FROM tag_cooccurrence");
        jdbc.execute("DELETE FROM artist_tags");
        jdbc.execute("DELETE FROM similar_artists");
        jdbc.execute("DELETE FROM artists");
        insertTestData();
    }

    @Test
    void computesTagPairsCorrectly() {
        JobExecution execution = jobLauncherTestUtils.launchStep("tagCooccurrenceStep");

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Artist A: metal + heavy  →  pair (heavy, metal): 1
        Integer heavyMetal = jdbc.queryForObject(
                "SELECT count FROM tag_cooccurrence WHERE tag_a = 'heavy' AND tag_b = 'metal'",
                Integer.class);
        assertThat(heavyMetal).isEqualTo(1);

        // Artist B: metal + death  →  pair (death, metal): 1
        Integer deathMetal = jdbc.queryForObject(
                "SELECT count FROM tag_cooccurrence WHERE tag_a = 'death' AND tag_b = 'metal'",
                Integer.class);
        assertThat(deathMetal).isEqualTo(1);

        // Artist C: heavy + doom  →  pair (doom, heavy): 1
        Integer doomHeavy = jdbc.queryForObject(
                "SELECT count FROM tag_cooccurrence WHERE tag_a = 'doom' AND tag_b = 'heavy'",
                Integer.class);
        assertThat(doomHeavy).isEqualTo(1);

        // Total distinct pairs = 3
        Integer totalPairs = jdbc.queryForObject("SELECT COUNT(*) FROM tag_cooccurrence", Integer.class);
        assertThat(totalPairs).isEqualTo(3);
    }

    @Test
    void idempotent_rerunProducesSameResult() {
        jobLauncherTestUtils.launchStep("tagCooccurrenceStep");
        jobRepositoryTestUtils.removeJobExecutions();

        JobExecution secondRun = jobLauncherTestUtils.launchStep("tagCooccurrenceStep");
        assertThat(secondRun.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        Integer totalPairs = jdbc.queryForObject("SELECT COUNT(*) FROM tag_cooccurrence", Integer.class);
        assertThat(totalPairs).isEqualTo(3);
    }

    private void insertTestData() {
        UUID artistA = insertArtist("BandA");
        UUID artistB = insertArtist("BandB");
        UUID artistC = insertArtist("BandC");

        insertTag(artistA, "metal", 100);
        insertTag(artistA, "heavy", 90);

        insertTag(artistB, "metal", 80);
        insertTag(artistB, "death", 70);

        insertTag(artistC, "heavy", 60);
        insertTag(artistC, "doom", 50);
    }

    private UUID insertArtist(String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO artists (id, name, created_at) VALUES (?, ?, NOW())", id, name);
        return id;
    }

    private void insertTag(UUID artistId, String tag, int weight) {
        jdbc.update("INSERT INTO artist_tags (artist_id, tag_name, weight) VALUES (?, ?, ?)",
                artistId, tag, weight);
    }
}
