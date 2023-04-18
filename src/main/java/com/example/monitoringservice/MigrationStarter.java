package com.example.monitoringservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class MigrationStarter {

    private final String fixedRate = System.getenv("MIGRATION_INTERVAL") != null ? System.getenv("MIGRATION_INTERVAL") : "24";

    @Autowired
    ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Scheduled
    public void startMigrationChain() {
        threadPoolTaskScheduler.scheduleAtFixedRate(new ExecuteMigrationTask(), Duration.ofHours(Long.parseLong(fixedRate)));
    }
}
