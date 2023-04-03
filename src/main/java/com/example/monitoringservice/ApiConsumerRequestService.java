package com.example.monitoringservice;

import com.example.monitoringservice.mysql.repositories.ServiceTableRepository;
import com.example.monitoringservice.mysql.tables.ServiceTable;
import com.rabbitmq.client.Connection;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.util.Config;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.IntStream;

import static com.example.monitoringservice.RabbitmqApiUrlProvider.rabbitmqApiConsumersUrl;

@Component
public class ApiConsumerRequestService {

    @Autowired
    ServiceTableRepository serviceTableRepository;
    @Autowired
    Connection connection;

    @EventListener(ContextRefreshedEvent.class)
    public void requestServicesFromRabbitMq() {
        String IP = Objects.requireNonNull(connection).getAddress().getHostAddress();

        String requestUrl = rabbitmqApiConsumersUrl(IP);
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
        try {
            System.out.println("THIS IS K: " + determineNumberOfAvailableKubernetesNodes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int determineNumberOfAvailableKubernetesNodes() {
        ApiClient client = null;
        try {
            client = Config.defaultClient();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Configuration.setDefaultApiClient(client);

        CoreV1Api api = new CoreV1Api();

        try {
            V1NodeList nodeList = null;
            nodeList = api.listNode(null, null, null, null, null, null, null, null, 10, false);
            return nodeList.getItems().size();
        } catch (ApiException e) {
            System.out.println(e.getResponseBody());
        }
        return 0;
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
