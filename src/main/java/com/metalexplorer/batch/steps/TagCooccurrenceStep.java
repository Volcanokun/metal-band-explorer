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
public class TagCooccurrenceStep implements Tasklet {

    private final JdbcTemplate jdbc;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        // 全ペアを完全再計算するため DELETE + INSERT（H2/PostgreSQL 両対応）
        jdbc.update("DELETE FROM tag_cooccurrence");
        int inserted = jdbc.update("""
                INSERT INTO tag_cooccurrence (tag_a, tag_b, count)
                SELECT a.tag_name, b.tag_name, COUNT(*)
                FROM artist_tags a
                JOIN artist_tags b
                  ON a.artist_id = b.artist_id AND a.tag_name < b.tag_name
                GROUP BY a.tag_name, b.tag_name
                """);

        contribution.incrementWriteCount(inserted);
        log.info("TagCooccurrence computed {} pairs", inserted);
        return RepeatStatus.FINISHED;
    }
}
