package com.metalexplorer.batch.steps;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
@Slf4j
@RequiredArgsConstructor
public class ComputedSimilarityStep implements Tasklet {

    private final JdbcTemplate jdbc;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        // 相関サブクエリ形式（H2/PostgreSQL 両対応）
        int updated = jdbc.update("""
                UPDATE similar_artists
                SET computed_score = (
                    SELECT CAST(COUNT(*) AS FLOAT) / NULLIF(
                        (SELECT COUNT(DISTINCT tag_name) FROM artist_tags WHERE artist_id = similar_artists.artist_id)
                        + (SELECT COUNT(DISTINCT tag_name) FROM artist_tags WHERE artist_id = similar_artists.similar_artist_id)
                        - COUNT(*),
                        0
                    )
                    FROM artist_tags x
                    JOIN artist_tags y
                      ON x.tag_name = y.tag_name
                     AND x.artist_id = similar_artists.artist_id
                     AND y.artist_id = similar_artists.similar_artist_id
                )
                WHERE EXISTS (
                    SELECT 1 FROM artist_tags WHERE artist_id = similar_artists.artist_id
                )
                """);

        contribution.incrementWriteCount(updated);
        log.info("ComputedSimilarity updated {} rows", updated);
        return RepeatStatus.FINISHED;
    }
}
