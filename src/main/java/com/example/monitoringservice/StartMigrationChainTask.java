package com.example.monitoringservice;

import com.example.monitoringservice.config.MessagingConfig;
import com.example.monitoringservice.mysql.repositories.CommunicationTableRepository;
import com.example.monitoringservice.mysql.repositories.ServiceTableRepository;
import com.example.monitoringservice.mysql.tables.CommunicationTable;
import com.example.monitoringservice.mysql.tables.ServiceTable;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.util.ClientBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static java.math.BigDecimal.ZERO;

@Component
public class StartMigrationChainTask implements Runnable {
    // prepares all information using the database and ApplicationSystem object
    // forwards object to mapping service

    @Autowired
    private RabbitTemplate template;
    @Autowired
    private ServiceTableRepository serviceTableRepository;
    @Autowired
    private CommunicationTableRepository communicationTableRepository;

    @Override
    public void run() {
        System.out.println("Migration chain starts..");
        ApplicationSystem applicationSystem = new ApplicationSystem();
        List<ServiceTable> allServices = serviceTableRepository.findAll();
        List<CommunicationTable> allCommunications = communicationTableRepository.findAll();

        if (allServices != null) {
            for (ServiceTable entry : allServices) {
                if (!serviceAlreadyRegistered(entry, applicationSystem.getServices())) {
                    Service service = createService(entry.getServiceIp(), entry.getServicePort());
                    System.out.println("Service created: " + service.getIpAdresse());
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

        try {
            int k = determineNumberOfAvailableKubernetesNodes();
            System.out.println("THI IS K: " + k);
            applicationSystem.setNumberOfNodes(k);
            template.convertAndSend(MessagingConfig.INTERNAL_EXCHANGE, MessagingConfig.MAPPING_ROUTING_KEY, applicationSystem);
        } catch (ApiException e) {
            System.out.println("A problem occured when requesting the Kubernetes API.");
            System.out.println(e.getResponseBody());
        }

        System.out.println("Deleting all entries...");
        // delete all entries
        serviceTableRepository.deleteAll();
        communicationTableRepository.deleteAll();
    }


    private int determineNumberOfAvailableKubernetesNodes() throws ApiException {
        ApiClient client = null;
        try {
            client = ClientBuilder.cluster().build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Configuration.setDefaultApiClient(client);

        CoreV1Api api = new CoreV1Api();

        V1NodeList nodeList = api.listNode(null, null, null, null, null, null, null, null, 10, false);
        if (nodeList != null) {
            return nodeList.getItems().size();
        }
        return 0;
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
