package com.metalexplorer.batch.scheduler;

import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BatchScheduler {

    @Bean
    public JobDetail artistDiscoveryJobDetail() {
        return JobBuilder.newJob(ArtistDiscoveryQuartzJob.class)
                .withIdentity("artistDiscoveryJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger artistDiscoveryTrigger(JobDetail artistDiscoveryJobDetail) {
        // 毎日 02:00 JST = 17:00 UTC
        return TriggerBuilder.newTrigger()
                .forJob(artistDiscoveryJobDetail)
                .withIdentity("artistDiscoveryTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 17 * * ?")
                        .inTimeZone(java.util.TimeZone.getTimeZone("UTC")))
                .build();
    }
}
