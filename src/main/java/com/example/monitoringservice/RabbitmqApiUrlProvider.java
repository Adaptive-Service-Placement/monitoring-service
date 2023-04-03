package com.example.monitoringservice;

public class RabbitmqApiUrlProvider {

    private static final String PROTOCOL = "http://";
    private static final String RABBITMQ_PORT = "15672";
    private static final String VHOST = "%2F";

    public static String rabbitmqApiConsumersUrl(String ip) {
        return PROTOCOL + ip + ":" + RABBITMQ_PORT + "/api/consumers";
    }

    public static String rabbitmqApiConnectionsUrl(String ip) {
        return PROTOCOL + ip + ":" + RABBITMQ_PORT + "/api/connections";
    }

    public static String rabbitmqApiExchangeSource(String ip, String exchange) {
        return PROTOCOL + ip + ":" + RABBITMQ_PORT + "/api/exchanges/" + VHOST + "/" + exchange + "/bindings/source";
    }
}
