package com.example.monitoringmanager.consumer;

import com.example.monitoringmanager.MessagingInformation;
import com.example.monitoringmanager.Utils;
import com.example.monitoringmanager.config.MessagingConfig;
import com.example.monitoringmanager.mysql.repositories.CommunicationTableRepository;
import com.example.monitoringmanager.mysql.repositories.ServiceTableRepository;
import com.example.monitoringmanager.mysql.tables.CommunicationTable;
import com.example.monitoringmanager.mysql.tables.ServiceTable;
import com.rabbitmq.client.Connection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class ApplicationMessageConsumer {

    final static String RABBITMQ_PORT = "15672";

    @Autowired
    RabbitTemplate template;
    @Autowired
    Connection connection;
    @Autowired
    ServiceTableRepository serviceTableRepository;
    @Autowired
    CommunicationTableRepository communicationTableRepository;

    @RabbitListener(queues = MessagingConfig.QUEUE)
    public void consumeMessagingInformation(MessagingInformation messagingInformation) {
        System.out.println("Service: " + messagingInformation.getServiceIp());
        System.out.println("Message size: " + messagingInformation.getMessageSize().toString());

        System.out.println("THIS IS HOST ADDRESS: " + Objects.requireNonNull(connection).getAddress().getHostAddress());

        try {
            long start = System.currentTimeMillis();
            List<String> queuesDestinations = getDestination(messagingInformation.getExchange(), messagingInformation.getRoutingKey());
            for (String queueName : queuesDestinations) {
                String service1Address = getDestinationAddressWithQueue(queueName);
                String service2Address = getSourceAddressWithServiceIp(messagingInformation.getServiceIp());
                saveCommunication(service1Address, service2Address, messagingInformation.getMessageSize());
            }
            System.out.println(queuesDestinations);
            long end = System.currentTimeMillis();
            System.out.println("Round trip response time = " + (end - start) + " millis");
        } catch (Exception e) {
            System.out.println("Oops.. something went wrong!");
            e.printStackTrace();
        }
    }

    private String getDestinationAddressWithQueue(String queueName) {
        ServiceTable service = serviceTableRepository.findFirstByQueueName(queueName);
        if (service != null) {
            return service.getServiceIp() + ":" + service.getServicePort();
        }
        return requestConsumerFromQueue(queueName);
    }

    private String requestConsumerFromQueue(String queueName) {
        final String IP = Objects.requireNonNull(connection).getAddress().getHostAddress();
        String requestUrl = "http://" + IP + ":" + RABBITMQ_PORT + "/api/consumers";
        String json = Utils.getJsonResponseFromAPI(requestUrl);

        try {
            JSONArray jsonArray = new JSONArray(json);
            return IntStream.range(0, jsonArray.length())
                    .mapToObj(i -> mapToJsonObject(jsonArray, i))
                    .filter(Objects::nonNull)
                    .filter(jsonObject -> getQueueName(jsonObject) != null && Objects.equals(getQueueName(jsonObject), queueName))
                    .findFirst()
                    .map(jsonObject -> {
                        JSONObject channel_details = mapJsonObjectToChannelDetails(jsonObject);
                        if (channel_details != null) {
                            String ip = getPeerIp(channel_details);
                            String port = getPeerPort(channel_details);
                            saveService(ip, port, queueName);
                            return ip + ":" + port;
                        }
                        return null;
                    })
                    .orElse(null);

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveService(String ip, String port, String queueName) {
        ServiceTable service = new ServiceTable();
        service.setServiceIp(ip);
        service.setServicePort(port);
        service.setQueueName(queueName);

        serviceTableRepository.save(service);
    }

    private JSONObject mapJsonObjectToChannelDetails(JSONObject jsonObject) {
        try {
            return jsonObject.getJSONObject("channel_details");
        } catch (Exception e) {
            return null;
        }
    }

    private String getQueueName(JSONObject jsonObject) {
        try {
            JSONObject queue = jsonObject.getJSONObject("queue");
            return queue.getString("name");
        } catch (JSONException e) {
            return null;
        }
    }

    private String getSourceAddressWithServiceIp(String serviceIp) {
        ServiceTable service = serviceTableRepository.findFirstByServiceIp(serviceIp);

        if (service != null) {
            System.out.println("EIN SERVICE WURDE GEFUNDEN!");
            return serviceIp + ":" + service.getServicePort();
        }

        System.out.println("ES WURDE KEIN SERVICE GEFUNDEN!");

        String servicePort = requestPortWithHostIp(serviceIp);
        saveService(serviceIp, servicePort, "");

        return serviceIp + ":" + servicePort;
    }

    private String requestPortWithHostIp(String ip) {
        final String IP = Objects.requireNonNull(connection).getAddress().getHostAddress();
        String requestUrl = "http://" + IP + ":" + RABBITMQ_PORT + "/api/connections";
        String json = Utils.getJsonResponseFromAPI(requestUrl);

        try {
            JSONArray jsonArray = new JSONArray(json);
            return IntStream.range(0, jsonArray.length())
                    .mapToObj(i -> mapToJsonObject(jsonArray, i))
                    .filter(Objects::nonNull)
                    .filter(jsonObject -> ip.equals(getPeerIp(jsonObject)))
                    .map(this::getPeerPort)
                    .findFirst()
                    .orElse(null);

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getPeerIp(JSONObject jsonObject) {
        try {
            return jsonObject.getString("peer_host");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getPeerPort(JSONObject jsonObject) {
        try {
            return jsonObject.getString("peer_port");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveCommunication(String service1, String service2, BigDecimal messagesExchanged) {
        CommunicationTable communication = communicationTableRepository.findFirstByService1AndService2(service1, service2);

        if (communication != null) {
            communication.setMessages_exchanged(communication.getMessages_exchanged().add(messagesExchanged));
        } else {
            communication = new CommunicationTable();
            communication.setService1(service1);
            communication.setService2(service2);
            communication.setMessages_exchanged(messagesExchanged);
        }

        communicationTableRepository.save(communication);
    }

    private List<String> getDestination(String exchange, String routingKey) throws Exception {
        String IP = Objects.requireNonNull(connection).getAddress().getHostAddress();

        String requestUrl = "http://" + IP + ":" + RABBITMQ_PORT + "/api/exchanges/%2F/" + exchange + "/bindings/source";

        return getDestinationFromJsonAndRoutingKey(Utils.getJsonResponseFromAPI(requestUrl), routingKey);
    }

    private List<String> getDestinationFromJsonAndRoutingKey(String json, String routingKey) throws JSONException {
        JSONArray jsonArray = new JSONArray(json);
        return IntStream.range(0, jsonArray.length())
                .mapToObj(i -> mapToJsonObject(jsonArray, i))
                .filter(Objects::nonNull)
                .filter(jsonObject -> filterRoutingKey(jsonObject, routingKey))
                .map(this::mapJSONObjectToDestination)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private JSONObject mapToJsonObject(JSONArray jsonArray, int i) {
        try {
            return jsonArray.getJSONObject(i);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String mapJSONObjectToDestination(JSONObject jsonObject) {
        try {
            return jsonObject.getString("destination");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean filterRoutingKey(JSONObject jsonObject, String routingKey) {
        try {
            return jsonObject.get("routing_key").equals(routingKey);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }
}

