package com.example.monitoringmanager.mysql.tables;

import java.io.Serializable;
import java.util.Objects;

public class CommunicationTableId implements Serializable {
    private String service1;
    private String service2;

    public CommunicationTableId() {
    }

    public CommunicationTableId(String service1, String service2) {
        this.service1 = service1;
        this.service2 = service2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommunicationTableId that = (CommunicationTableId) o;
        return Objects.equals(service1, that.service1) && Objects.equals(service2, that.service2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service1, service2);
    }
}
