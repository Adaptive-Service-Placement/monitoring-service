package com.example.monitoringmanager;

import com.example.monitoringmanager.mysql.repositories.ServiceTableRepository;
import com.example.monitoringmanager.mysql.tables.ServiceTable;
import com.rabbitmq.client.Connection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.stream.IntStream;

@Component
public class ApiConsumerRequestService {

    final static String PORT = "15672";

    @Autowired
    ServiceTableRepository serviceTableRepository;
    @Autowired
    Connection connection;

    @EventListener(ContextRefreshedEvent.class)
    public void requestServicesFromRabbitMq() {
        String IP = Objects.requireNonNull(connection).getAddress().getHostAddress();

        String requestUrl = "http://" + IP + ":" + PORT + "/api/consumers";
        String json = Utils.getJsonResponseFromAPI(requestUrl);
        // TODO: filter mapping, monitoring and migration service
        try {
            JSONArray jsonArray = new JSONArray(json);
            IntStream.range(0, jsonArray.length())
                    .mapToObj(i -> mapToJsonObject(jsonArray, i))
                    .filter(Objects::nonNull)
                    .forEach(jsonObject -> {
                        String hostIp = getPeerIp(jsonObject);
                        String hostPort = getPeerPort(jsonObject);
                        String queueName = getQueueName(jsonObject);
                        if (hostIp != null && hostPort != null && queueName != null) {
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
