package com.example.monitoringservice.consumer;

import com.example.monitoringservice.ApplicationSystem;
import com.example.monitoringservice.Connection;
import com.example.monitoringservice.Service;
import com.example.monitoringservice.config.MessagingConfig;
import com.example.monitoringservice.dto.WakeupCall;
import com.example.monitoringservice.mysql.repositories.CommunicationTableRepository;
import com.example.monitoringservice.mysql.repositories.ServiceTableRepository;
import com.example.monitoringservice.mysql.tables.CommunicationTable;
import com.example.monitoringservice.mysql.tables.ServiceTable;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static java.math.BigDecimal.ZERO;

@Component
public class StartMigrationRequestConsumer {
    // listens to specific queue, once a message is received, the migration phasis starts
    // prepares all information using the database and ApplicationSystem object
    // forwards object to mapping service

    @Autowired
    RabbitTemplate template;
    @Autowired
    ServiceTableRepository serviceTableRepository;
    @Autowired
    CommunicationTableRepository communicationTableRepository;

    @RabbitListener(queues = MessagingConfig.START_MIGRATION_QUEUE)
    public void handleStartMigrationRequest(WakeupCall wakeupCall) {
        System.out.println("WAKEUP CALL: " + wakeupCall.getWakeupMessage());
        ApplicationSystem applicationSystem = new ApplicationSystem();
        List<ServiceTable> allServices = serviceTableRepository.findAll();
        List<CommunicationTable> allCommunications = communicationTableRepository.findAll();

        if (allServices != null) {
            for (ServiceTable entry : allServices) {
                if (!serviceAlreadyRegistered(entry, applicationSystem.getServices())) {
                    Service service = createService(entry.getServiceIp(), entry.getServicePort());
                    applicationSystem.getServices().add(service);
                }
            }
        }
        BigDecimal bytesExchangedTotal = ZERO;
        if (allCommunications != null) {
            for (CommunicationTable entry : allCommunications) {
                Connection connection = createConnection(entry, applicationSystem);

                if (entry.getMessages_exchanged() != null) {
                    bytesExchangedTotal = bytesExchangedTotal.add(entry.getMessages_exchanged());
                }

                if (connection != null) {
                    applicationSystem.getConnections().add(connection);
                }
            }
        }

        applicationSystem.setBytesExchangedTotal(bytesExchangedTotal);

        template.convertAndSend(MessagingConfig.INTERNAL_EXCHANGE, MessagingConfig.MAPPING_ROUTING_KEY, applicationSystem);

        // TODO: delete all entries in table
    }

    private Connection createConnection(CommunicationTable entry, ApplicationSystem applicationSystem) {
        String service1Ip = entry.getService1().split(":")[0];
        String service1Port = entry.getService1().split(":")[1];
        Service service1 = findServiceinSystem(applicationSystem.getServices(), service1Ip, service1Port);

        String service2Ip = entry.getService2().split(":")[0];
        String service2Port = entry.getService2().split(":")[1];
        Service service2 = findServiceinSystem(applicationSystem.getServices(), service2Ip, service2Port);

        if (service1 != null && service2 != null) {
            Connection connection = new Connection();
            connection.setService1(service1);
            connection.setService2(service2);
            connection.setBytesExchanged(entry.getMessages_exchanged() != null ? entry.getMessages_exchanged() : ZERO);

            return connection;
        }

        return null;
    }

    private Service findServiceinSystem(List<Service> services, String serviceIp, String servicePort) {
        return services.stream()
                .filter(service -> serviceIp.equals(service.getIpAdresse()) && servicePort.equals(service.getPort()))
                .findFirst()
                .orElse(null);
    }

    private boolean serviceAlreadyRegistered(ServiceTable entry, List<Service> services) {
        return services.stream()
                .anyMatch(service -> entry.getServiceIp().equals(service.getIpAdresse()) && entry.getServicePort().equals(service.getPort()));
    }

    private Service createService(String ip, String port) {
        Service service = new Service();
        service.setId(createId());
        service.setIpAdresse(ip);
        service.setPort(port);
        return service;
    }

    private String createId() {
        return UUID.randomUUID().toString();
    }
}
