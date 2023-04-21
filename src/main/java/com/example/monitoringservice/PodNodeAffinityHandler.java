package com.example.monitoringservice;

import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Watch;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

public class PodNodeAffinityHandler {

    private final Map<V1Node, List<V1Pod>> podNodeAssignement;
    private final CoreV1Api api;
    private final AppsV1Api appsV1Api;

    public PodNodeAffinityHandler(CoreV1Api api, AppsV1Api appsV1Api, Map<V1Node, List<V1Pod>> podNodeAssignement) {
        this.api = api;
        this.podNodeAssignement = podNodeAssignement;
        this.appsV1Api = appsV1Api;
    }

    public void setAllAffinities() throws ApiException {
        int index = 0;
        for (Entry<V1Node, List<V1Pod>> entry : podNodeAssignement.entrySet()) {
            List<V1Pod> groupedPods = entry.getValue();
            V1Node destinedNode = entry.getKey();

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

//                V1NodeSelectorRequirement nodeSelectorRequirement = new V1NodeSelectorRequirement()
//                        .key("kubernetes.io/hostname")
//                        .operator("In")
//                        .values(Collections.singletonList(destinedNode.getMetadata().getName()));
//
//                V1NodeSelectorTerm nodeSelectorTerm = new V1NodeSelectorTerm()
//                        .matchExpressions(Collections.singletonList(nodeSelectorRequirement));
//
//                V1NodeSelector nodeSelector = new V1NodeSelector()
//                        .nodeSelectorTerms(Collections.singletonList(nodeSelectorTerm));
//
//                V1Affinity affinity = new V1Affinity()
//                        .nodeAffinity(new V1NodeAffinity().requiredDuringSchedulingIgnoredDuringExecution(nodeSelector));
//
//                pod.getSpec().affinity(affinity);

                String releaseLabelValue = pod.getMetadata().getLabels() != null ? pod.getMetadata().getLabels().get("release") : "";

                V1DeploymentList deployments = appsV1Api.listNamespacedDeployment("default", null, null, null, null, "release=" + releaseLabelValue, null, null, null, null, null);
                if (deployments != null) {
                    System.out.println("Deployment gefunden!");
                    V1Deployment oldDeployment = deployments.getItems().get(0);
                    V1Deployment newDeployment = clone(oldDeployment);
//                    debugPrintDeployment(oldDeployment);
                    V1PodSpec podSpec = getPodSpecFromDeploymentNullsafe(newDeployment);
                    if (podSpec != null) {
                        podSpec.putNodeSelectorItem("kubernetes.io/hostname", destinedNode.getMetadata().getName());
                    }
                    if (oldDeployment.getMetadata() != null) {
                        appsV1Api.deleteNamespacedDeployment(oldDeployment.getMetadata().getName(), "default", null, null, 0, null, "Background", null);
                        appsV1Api.createNamespacedDeployment("default", newDeployment, null, null, null, null);
                    }
                } else {
                    System.out.println("No Deployment found!");
                }


//                if (pod.getSpec().getNodeName() != null && destinedNode.getMetadata().getName() != null && pod.getSpec().getNodeName().equals(destinedNode.getMetadata().getName())) {
////                    pod.getSpec().putNodeSelectorItem("kubernetes.io/hostname", destinedNode.getMetadata().getName());
//                    pod.getSpec().setNodeName(destinedNode.getMetadata().getName());
//
//                    System.out.println("Moving Pod: " + pod.getMetadata().getName() + "to Node: " + destinedNode.getMetadata().getName());
////
//                    api.deleteNamespacedPod(Objects.requireNonNull(pod.getMetadata()).getName(), "default", null, null, 0, null, "Background", null);
//
//                    api.createNamespacedPod("default", pod, null, null, null, null);
//                }


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

    private V1PodSpec getPodSpecFromDeploymentNullsafe(V1Deployment deployment) {
        if (deployment != null && deployment.getSpec() != null && deployment.getSpec() != null && deployment.getSpec().getTemplate() != null) {
            return deployment.getSpec().getTemplate().getSpec();
        }
        return null;
    }

    private void debugPrintDeployment(V1Deployment oldDeployment) {
        if (oldDeployment != null && oldDeployment.getSpec() != null) {
            System.out.println("V1DeploymentSpec not null!");
            V1DeploymentSpec spec = oldDeployment.getSpec();
            if (spec.getTemplate() != null) {
                System.out.println("V1PodTemplateSpec not null!");
                V1PodTemplateSpec templateSpec = spec.getTemplate();
                if (templateSpec.getSpec() != null) {
                    System.out.println("V1PodSpec not null!");
                    V1PodSpec podSpec = templateSpec.getSpec();
                    System.out.println(podSpec);
                } else {
                    System.out.println("V1PodSpec is null!");
                }
            } else {
                System.out.println("V1PodTemplateSpec is null!");
            }
        } else {
            System.out.println("V1DeploymentSpec is null!");
        }
    }

    private V1Deployment clone(V1Deployment deploymentToClone) {
        V1Deployment clonedDeployment = new V1Deployment();
        clonedDeployment.setApiVersion(deploymentToClone.getApiVersion());
        clonedDeployment.setKind(deploymentToClone.getKind());
        clonedDeployment.setMetadata(deploymentToClone.getMetadata());
        if (clonedDeployment.getMetadata() != null) {
            clonedDeployment.getMetadata().setResourceVersion(null);
        }
        clonedDeployment.setSpec(deploymentToClone.getSpec());
        clonedDeployment.setStatus(deploymentToClone.getStatus());

        return clonedDeployment;
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
