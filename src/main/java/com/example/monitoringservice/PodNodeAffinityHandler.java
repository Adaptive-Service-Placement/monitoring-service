package com.example.monitoringservice;

import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Watch;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

public class PodNodeAffinityHandler {

    private final Map<V1Node, List<V1Pod>> podNodeAssignement;
    private final CoreV1Api api;

    public PodNodeAffinityHandler(CoreV1Api api, Map<V1Node, List<V1Pod>> podNodeAssignement) {
        this.api = api;
        this.podNodeAssignement = podNodeAssignement;
    }

    public void setAllAffinities() throws ApiException {
        int index = 0;
        for (Entry<V1Node, List<V1Pod>> entry : podNodeAssignement.entrySet()) {
            List<V1Pod> groupedPods = entry.getValue();
            V1Node destinedNode = entry.getKey();

            String key = "group";
            String value = "group" + index;

            for (V1Pod pod : groupedPods) {
                if (pod.getMetadata() == null || destinedNode.getMetadata() == null || pod.getSpec() == null) {
                    continue;
                }
//                this.setLabel(pod, key, value);
//                V1Affinity affinity = new V1Affinity();
//
//                V1PodAffinityTerm podAffinityTerm = new V1PodAffinityTerm();
//                podAffinityTerm.setLabelSelector(new V1LabelSelector().putMatchLabelsItem(key, value));
//                podAffinityTerm.setTopologyKey("kubernetes.io/hostname");
//
//                V1PodAffinity podAffinity = new V1PodAffinity();
//                podAffinity.addRequiredDuringSchedulingIgnoredDuringExecutionItem(podAffinityTerm);
//
//                affinity.setPodAffinity(podAffinity);
//
//                if (pod.getSpec() != null) {
//                    pod.setSpec(pod.getSpec().affinity(affinity));
//                }
//
//                System.out.println("Pod's labels:");
//                for (Entry<String, String> k : Objects.requireNonNull(pod.getMetadata().getLabels()).entrySet()) {
//                    System.out.println(k.getKey() + ":" + k.getValue());
//                }

                V1NodeSelectorRequirement nodeSelectorRequirement = new V1NodeSelectorRequirement()
                        .key("kubernetes.io/hostname")
                        .operator("In")
                        .values(Collections.singletonList(destinedNode.getMetadata().getName()));

                V1NodeSelectorTerm nodeSelectorTerm = new V1NodeSelectorTerm()
                        .matchExpressions(Collections.singletonList(nodeSelectorRequirement));

                V1NodeSelector nodeSelector = new V1NodeSelector()
                        .nodeSelectorTerms(Collections.singletonList(nodeSelectorTerm));

                V1Affinity affinity = new V1Affinity()
                        .nodeAffinity(new V1NodeAffinity().requiredDuringSchedulingIgnoredDuringExecution(nodeSelector));

                pod.getSpec().affinity(affinity);


//                pod.getSpec().setNodeName(destinedNode.getMetadata().getName());
//
                api.deleteNamespacedPod(Objects.requireNonNull(pod.getMetadata()).getName(), "default", null, null, 0, null, "Background", null);

                api.createNamespacedPod("default", pod, null, null, null, null);

                System.out.println("Restartet Pod: " + pod.getMetadata().getName());


//                replacePodOnceTerminated(pod);
//                api.replaceNamespacedPod(pod.getMetadata().getName(), "default", pod, null, null, null, null);
                // create the patch object
//                V1Patch patch = new V1Patch("[{\"op\": \"replace\", \"path\": \"/spec/nodeName\", \"value\": \"" + destinedNode.getMetadata().getName() + "\"}]");
//                V1Pod patchedPod = api.patchNamespacedPod(pod.getMetadata().getName(), "default", patch, null, null, null, null, true);
//
//                System.out.println("Updated pod " + pod.getMetadata().getName() + " to run on node " + Objects.requireNonNull(patchedPod.getSpec()).getNodeName());

            }
            index++;
        }
    }

    private void replacePodOnceTerminated(V1Pod pod) throws ApiException {
        api.deleteNamespacedPod(Objects.requireNonNull(pod.getMetadata()).getName(), "default", null, null, null, null, null, null);

        Watch<V1Pod> watch = Watch.createWatch(api.getApiClient(),
                api.listNamespacedPodCall("default", null, null, null, null, null, null, null, null, 10, true, null),
                new TypeToken<Watch.Response<V1Pod>>() {
                }.getType());

        for (Watch.Response<V1Pod> event : watch) {
            System.out.println("Waiting for events ...");
            V1Pod updatedPod = event.object;
            V1ObjectMeta meta = updatedPod.getMetadata();
            if (Objects.equals(Objects.requireNonNull(meta).getName(), Objects.requireNonNull(pod.getMetadata()).getName())) {
                System.out.println("Test does this even happen?");
                System.out.println("This is the status: " + Objects.requireNonNull(updatedPod.getStatus()).getPhase());
                if ("Succeeded".equals(Objects.requireNonNull(updatedPod.getStatus()).getPhase())) {
                    System.out.println("Oh Status hase been changed!");
                    api.createNamespacedPod("default", pod, null, null, null, null);
                }
            }
        }
    }

    private void patchPod(V1Pod pod, String key, String value) throws ApiException {
        if (pod.getMetadata() != null) {
            String patchJson = "{\"metadata\":{\"labels\":{\"" + key + "\":\"" + value + "\"}}}";
            V1Patch patch = new V1Patch(patchJson);
            api.patchNamespacedPod(pod.getMetadata().getName(), "default", patch, null, null, null, null, null);
        }
    }


    private void setLabel(V1Pod pod, String key, String value) {
        if (pod.getMetadata() != null && pod.getMetadata().getLabels() != null) {
            Map<String, String> labels = pod.getMetadata().getLabels();

            labels.put(key, value);
        }
    }
}
