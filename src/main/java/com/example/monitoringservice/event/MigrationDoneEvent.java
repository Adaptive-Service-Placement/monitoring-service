package com.example.monitoringservice.event;

import org.springframework.context.ApplicationEvent;

public class MigrationDoneEvent extends ApplicationEvent {
    public MigrationDoneEvent(Object source) {
        super(source);
    }
}
