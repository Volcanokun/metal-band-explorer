package com.metalexplorer.batch.steps;

import com.metalexplorer.mapper.TagCooccurrenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
@RequiredArgsConstructor
public class TagCooccurrenceStep implements Tasklet {

    private final TagCooccurrenceMapper tagCooccurrenceMapper;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        tagCooccurrenceMapper.deleteAll();
        int inserted = tagCooccurrenceMapper.insertFromArtistTags();
        contribution.incrementWriteCount(inserted);
        log.info("TagCooccurrence computed {} pairs", inserted);
        return RepeatStatus.FINISHED;
    }
}
