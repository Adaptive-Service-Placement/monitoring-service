package com.example.monitoringservice;

import com.example.monitoringservice.mysql.repositories.ServiceTableRepository;
import com.example.monitoringservice.mysql.tables.ServiceTable;
import com.rabbitmq.client.Connection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static com.example.monitoringservice.RabbitmqApiUrlProvider.rabbitmqApiConsumersUrl;
import static com.example.monitoringservice.config.MessagingConfig.MIGRATION_FINISHED_QUEUE;
import static com.example.monitoringservice.config.MessagingConfig.MONITORING_QUEUE;
import static java.util.List.of;

@Component
public class ApiConsumerRequestService {

    private static final List<String> INTERNAL_QUEUES = of(MIGRATION_FINISHED_QUEUE, MONITORING_QUEUE, "monitoring.mapping.migration", "monitoring.mapping");

    @Autowired
    ServiceTableRepository serviceTableRepository;
    @Autowired
    Connection connection;

    @EventListener(ContextRefreshedEvent.class)
    public void requestServicesFromRabbitMq() {
        String IP = Objects.requireNonNull(connection).getAddress().getHostAddress();
        System.out.println("Migration Interval: " + System.getenv("MIGRATION_INTERVAL"));

        String requestUrl = rabbitmqApiConsumersUrl(IP);
        String json = Utils.getJsonResponseFromAPI(requestUrl);

        try {
            JSONArray jsonArray = new JSONArray(json);
            IntStream.range(0, jsonArray.length())
                    .mapToObj(i -> mapToJsonObject(jsonArray, i))
                    .filter(Objects::nonNull)
                    .forEach(jsonObject -> {
                        String hostIp = getPeerIp(jsonObject);
                        String hostPort = getPeerPort(jsonObject);
                        String queueName = getQueueName(jsonObject);
                        if (hostIp != null && hostPort != null && queueName != null && !INTERNAL_QUEUES.contains(queueName)) {
                            System.out.println("Detected Service: " + hostIp + ":" + hostPort);
                            System.out.println("Listens to: " + queueName);
                            ServiceTable service = new ServiceTable();
                            service.setServiceIp(hostIp);
                            service.setServicePort(hostPort);
                            service.setQueueName(queueName);
                            serviceTableRepository.save(service);
                        }
                    });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String getPeerIp(JSONObject jsonObject) {
        try {
            JSONObject channelDetails = jsonObject.getJSONObject("channel_details");
            return channelDetails.getString("peer_host");
        } catch (JSONException e) {
            return null;
        }
    }

    private String getPeerPort(JSONObject jsonObject) {
        try {
            JSONObject channelDetails = jsonObject.getJSONObject("channel_details");
            return channelDetails.getString("peer_port");
        } catch (JSONException e) {
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

    private JSONObject mapToJsonObject(JSONArray jsonArray, int i) {
        try {
            return jsonArray.getJSONObject(i);
        } catch (JSONException e) {
            return null;
        }
    }
}
