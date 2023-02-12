package com.example.monitoringmanager.mapping;

import com.example.monitoringmanager.ApplicationSystem;
import com.example.monitoringmanager.Connection;
import com.example.monitoringmanager.Service;
import com.example.monitoringmanager.config.MessagingConfig;
import com.example.monitoringmanager.dto.WakeupCall;
import com.example.monitoringmanager.mysql.repositories.CommunicationTableRepository;
import com.example.monitoringmanager.mysql.repositories.ServiceTableRepository;
import com.example.monitoringmanager.mysql.tables.CommunicationTable;
import com.example.monitoringmanager.mysql.tables.ServiceTable;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

import static java.math.BigDecimal.ZERO;

@Component
public class Mapper {
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
    public void receiveMigrationCall(WakeupCall wakeupCall) {
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
                String service1Ip = entry.getService1().split(":")[0];
                String service1Port = entry.getService1().split(":")[1];
                Service service1 = createService(service1Ip, service1Port);

                String service2Ip = entry.getService2().split(":")[0];
                String service2Port = entry.getService2().split(":")[1];
                Service service2 = createService(service2Ip, service2Port);

                Connection connection = createConnection(service1, service2, entry.getMessages_exchanged());

                if (entry.getMessages_exchanged() != null) {
                    bytesExchangedTotal = bytesExchangedTotal.add(entry.getMessages_exchanged());
                }

                applicationSystem.getConnections().add(connection);
            }
        }

        applicationSystem.setBytesExchangedTotal(bytesExchangedTotal);

        template.convertAndSend(MessagingConfig.EXCHANGE, MessagingConfig.MAPPING_ROUTING_KEY, applicationSystem);
    }

    private boolean serviceAlreadyRegistered(ServiceTable entry, List<Service> services) {
        return services.stream()
                .anyMatch(service -> entry.getServiceIp().equals(service.getIpAdresse()) && entry.getServicePort().equals(service.getPort()));
    }

    private Service createService(String ip, String port) {
        Service service = new Service();
        service.setIpAdresse(ip);
        service.setPort(port);
        return service;
    }

    private Connection createConnection(Service service1, Service service2, BigDecimal bytesExchanged) {
        Connection connection = new Connection();
        connection.setService1(service1);
        connection.setService2(service2);
        connection.setBytesExchanged(bytesExchanged);
        return connection;
    }
}
