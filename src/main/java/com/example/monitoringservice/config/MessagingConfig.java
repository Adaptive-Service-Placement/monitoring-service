package com.example.monitoringservice.config;

import com.rabbitmq.client.Connection;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {

    public static final String MONITORING_QUEUE = "monitoring_queue";
    public static final String APPLICATION_EXCHANGE = "bachelor_exchange";
    public static final String MONITORING_ROUTING_KEY = "monitoring_routingKey";

    public static final String INTERNAL_EXCHANGE = "internal_exchange";
    public static final String MAPPING_ROUTING_KEY = "mapping_routingkey";

    public static final String MIGRATION_FINISHED_QUEUE = "finished.migration";
    public static final String MIGRATION_FINISHED_ROUTING_KEY = "finished_routingkey";

    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue monitoringQueue() {
        return new Queue(MONITORING_QUEUE);
    }

    @Bean
    public Queue finishedMigrationQueue() {
        return new Queue(MIGRATION_FINISHED_QUEUE);
    }

    @Bean
    public TopicExchange applicationExchange() {
        return new TopicExchange(APPLICATION_EXCHANGE);
    }

    @Bean
    public TopicExchange internalExchange() {
        return new TopicExchange(INTERNAL_EXCHANGE);
    }

    @Bean
    public Binding monitoringBinding(Queue monitoringQueue, TopicExchange applicationExchange) {
        return BindingBuilder.bind(monitoringQueue).to(applicationExchange).with(MONITORING_ROUTING_KEY);
    }

    @Bean
    public Binding finishedMigrationBinding(Queue finishedMigrationQueue, TopicExchange internalExchange) {
        return BindingBuilder.bind(finishedMigrationQueue).to(internalExchange).with(MIGRATION_FINISHED_ROUTING_KEY);
    }

    @Bean
    public AmqpTemplate template(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }

    @Bean
    public Connection connection(RabbitTemplate template) {
        return template.getConnectionFactory().createConnection().getDelegate();
    }
}
