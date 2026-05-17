package com.metalexplorer.batch;

import com.metalexplorer.batch.steps.*;
import com.metalexplorer.mapper.SimilarArtistMapper;
import com.metalexplorer.mapper.TagCooccurrenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class ArtistDiscoveryJob {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final BatchJobMetricsListener metricsListener;

    @Bean
    public Job discoveryJob(Step artistEnrichStep,
                             Step tagCooccurrenceStep,
                             Step computedSimilarityStep) {
        return new JobBuilder("artistDiscoveryJob", jobRepository)
                .listener(metricsListener)
                .start(artistEnrichStep)
                .next(tagCooccurrenceStep)
                .next(computedSimilarityStep)
                .build();
    }

    @Bean
    public Step artistEnrichStep(SeedArtistReader reader,
                                  LastfmEnrichProcessor processor,
                                  ArtistDbWriter writer) {
        return new StepBuilder("artistEnrichStep", jobRepository)
                .<String, EnrichedArtist>chunk(50, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(10)
                .build();
    }

    @Bean
    public TagCooccurrenceStep tagCooccurrenceTasklet(TagCooccurrenceMapper tagCooccurrenceMapper) {
        return new TagCooccurrenceStep(tagCooccurrenceMapper);
    }

    @Bean
    public Step tagCooccurrenceStep(TagCooccurrenceStep tagCooccurrenceTasklet) {
        return new StepBuilder("tagCooccurrenceStep", jobRepository)
                .tasklet(tagCooccurrenceTasklet, transactionManager)
                .build();
    }

    @Bean
    public ComputedSimilarityStep computedSimilarityTasklet(SimilarArtistMapper similarArtistMapper) {
        return new ComputedSimilarityStep(similarArtistMapper);
    }

    @Bean
    public Step computedSimilarityStep(ComputedSimilarityStep computedSimilarityTasklet) {
        return new StepBuilder("computedSimilarityStep", jobRepository)
                .tasklet(computedSimilarityTasklet, transactionManager)
                .build();
    }
}
