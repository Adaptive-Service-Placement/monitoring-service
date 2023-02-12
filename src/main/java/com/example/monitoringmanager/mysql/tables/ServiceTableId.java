package com.example.monitoringmanager.mysql.tables;

import java.io.Serializable;
import java.util.Objects;

public class ServiceTableId implements Serializable {
    private String serviceIp;
    private String servicePort;
    private String queueName;

    public ServiceTableId() {
    }

    public ServiceTableId(String serviceIp, String servicePort, String queueName) {
        this.serviceIp = serviceIp;
        this.servicePort = servicePort;
        this.queueName = queueName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceTableId that = (ServiceTableId) o;
        return Objects.equals(serviceIp, that.serviceIp) && Objects.equals(servicePort, that.servicePort) && Objects.equals(queueName, that.queueName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceIp, servicePort, queueName);
    }
}
