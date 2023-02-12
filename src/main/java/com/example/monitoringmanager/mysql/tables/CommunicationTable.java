package com.example.monitoringmanager.mysql.tables;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import java.math.BigDecimal;

@Entity
@IdClass(CommunicationTableId.class)
public class CommunicationTable {
    @Id
    private String service1;
    @Id
    private String service2;
    private BigDecimal messages_exchanged;

    public String getService1() {
        return service1;
    }

    public void setService1(String service1) {
        this.service1 = service1;
    }

    public String getService2() {
        return service2;
    }

    public void setService2(String service2) {
        this.service2 = service2;
    }

    public BigDecimal getMessages_exchanged() {
        return messages_exchanged;
    }

    public void setMessages_exchanged(BigDecimal messages_exchanged) {
        this.messages_exchanged = messages_exchanged;
    }
}
