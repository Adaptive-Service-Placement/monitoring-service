package com.example.monitoringservice;

import com.example.monitoringservice.mysql.repositories.ServiceTableRepository;
import com.example.monitoringservice.mysql.tables.ServiceTable;
import com.rabbitmq.client.Connection;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.ClientBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

import static com.example.monitoringservice.RabbitmqApiUrlProvider.rabbitmqApiConsumersUrl;
import static java.util.List.of;

@Component
public class ApiConsumerRequestService {

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
            testPodNodeAffinityHandler();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void testPodNodeAffinityHandler() {
        ApiClient client = null;
        try {
            client = ClientBuilder.cluster().build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Configuration.setDefaultApiClient(client);

        CoreV1Api api = new CoreV1Api();

        Map<V1Node, List<V1Pod>> assignement = new HashMap<>();

        List<V1Node> allNodes = getKubernetesNodes(api);
        System.out.println("All nodes: " + printNodes(allNodes));
        List<V1Pod> allPods = getKubernetesPods(api);
        System.out.println("All pods: " + printPods(allPods));

        if (allNodes.size() >= 3 && allPods.size() >= 5) {
            System.out.println("Anzahl and Nodes und Pods passt.");
            System.out.println("Setting.. " + Objects.requireNonNull(allPods.get(0).getMetadata()).getName() + " and " + Objects.requireNonNull(allPods.get(1).getMetadata()).getName());
            assignement.put(allNodes.get(0), of(clone(allPods.get(0)), clone(allPods.get(1))));

            System.out.println("Setting.. " + Objects.requireNonNull(allPods.get(2).getMetadata()).getName() + " and " + Objects.requireNonNull(allPods.get(3).getMetadata()).getName());
            assignement.put(allNodes.get(1), of(clone(allPods.get(2)), clone(allPods.get(3))));

            System.out.println("Setting.. " + Objects.requireNonNull(allPods.get(4).getMetadata()).getName());
            assignement.put(allNodes.get(2), of(clone(allPods.get(4))));
        }

        PodNodeAffinityHandler handler = new PodNodeAffinityHandler(api, assignement);
        try {
            handler.setAllAffinities();
        } catch (ApiException e) {
            System.out.println("Something went wrong!");
            System.out.println(e.getResponseBody());
        }
    }

    private V1Pod clone(V1Pod pod) {
        V1Pod clone = new V1Pod();
        clone.setMetadata(pod.getMetadata());
        Objects.requireNonNull(pod.getMetadata()).setResourceVersion(null);
        clone.setSpec(pod.getSpec());
        clone.setApiVersion(pod.getApiVersion());
        clone.setKind(pod.getKind());
        clone.setStatus(pod.getStatus());

        return clone;
    }

    private String printPods(List<V1Pod> allPods) {
        return allPods.stream()
                .map(pod -> Objects.requireNonNull(pod.getMetadata()).getName())
                .filter(Objects::nonNull)
                .reduce(String::concat)
                .orElse("");
    }

    private String printNodes(List<V1Node> allNodes) {
        return allNodes.stream()
                .map(node -> Objects.requireNonNull(node.getMetadata()).getName())
                .filter(Objects::nonNull)
                .reduce(String::concat)
                .orElse("");
    }

    private List<V1Pod> getKubernetesPods(CoreV1Api api) {
        try {
            return api.listNamespacedPod("default", null, null, null, null, null, null, null, null, 10, false).getItems();
        } catch (ApiException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    private List<V1Node> getKubernetesNodes(CoreV1Api api) {
        try {
            return api.listNode(null, null, null, null, null, null, null, null, 10, false).getItems();
        } catch (ApiException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    private int determineNumberOfAvailableKubernetesNodes() {
        ApiClient client = null;
        try {
            client = ClientBuilder.cluster().build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Configuration.setDefaultApiClient(client);

        CoreV1Api api = new CoreV1Api();

        try {
            V1NodeList nodeList = api.listNode(null, null, null, null, null, null, null, null, 10, false);
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
