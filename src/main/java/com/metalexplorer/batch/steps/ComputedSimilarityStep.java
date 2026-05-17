package com.metalexplorer.batch.steps;

import com.metalexplorer.mapper.SimilarArtistMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
@RequiredArgsConstructor
public class ComputedSimilarityStep implements Tasklet {

    private final SimilarArtistMapper similarArtistMapper;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        int updated = similarArtistMapper.updateComputedScores();
        contribution.incrementWriteCount(updated);
        log.info("ComputedSimilarity updated {} rows", updated);
        return RepeatStatus.FINISHED;
    }
}
