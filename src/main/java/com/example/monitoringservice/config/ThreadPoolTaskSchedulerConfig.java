package com.example.monitoringservice.config;

import com.example.monitoringservice.ExecuteMigrationTask;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final String MINUTES_PER_DAY = "1440";

    private final String fixedRate = System.getenv("MIGRATION_INTERVAL") != null ? System.getenv("MIGRATION_INTERVAL") : MINUTES_PER_DAY;

    @Autowired
    ExecuteMigrationTask executeMigrationTask;

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
        taskRegistrar.addFixedRateTask(executeMigrationTask, Duration.ofMinutes(Long.parseLong(fixedRate)));
    }
}
