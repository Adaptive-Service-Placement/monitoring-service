package com.example.monitoringservice.consumer;

import com.example.monitoringservice.config.MessagingConfig;
import com.example.monitoringservice.dto.MigrationFinishedMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ContextRefreshedEvent;

public class MigrationFinishedMessageConsumer {

    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    ApplicationEventPublisher applicationEventPublisher;
    @Autowired
    RabbitTemplate template;

    @RabbitListener(queues = MessagingConfig.MIGRATION_FINISHED_QUEUE)
    public void consumeMigrationFinishedMessage(MigrationFinishedMessage migrationFinishedMessage) {
        if (migrationFinishedMessage != null && migrationFinishedMessage.isMigrationSuccessful()) {
            applicationEventPublisher.publishEvent(new ContextRefreshedEvent(applicationContext));
        }
    }
}
