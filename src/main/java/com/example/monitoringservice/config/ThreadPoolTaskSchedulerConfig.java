package com.example.monitoringservice.config;

import com.example.monitoringservice.ExecuteMigrationTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Duration;

@EnableScheduling
@Configuration
public class ThreadPoolTaskSchedulerConfig implements SchedulingConfigurer {

    private final String fixedRate = System.getenv("MIGRATION_INTERVAL") != null ? System.getenv("MIGRATION_INTERVAL") : "24";

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler
                = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setThreadNamePrefix(
                "ThreadPoolTaskScheduler");
        return threadPoolTaskScheduler;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addFixedRateTask(new ExecuteMigrationTask(), Duration.ofHours(Long.parseLong(fixedRate)));
    }
}
