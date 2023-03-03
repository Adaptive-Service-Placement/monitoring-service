package com.example.monitoringmanager.config;

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

    public static final String QUEUE = "monitoring_queue";
    public static final String EXCHANGE = "bachelor_exchange";
    public static final String MONITORING_ROUTING_KEY = "monitoring_routingKey";

    public static final String INTERNAL_EXCHANGE = "internal_exchange";
    public static final String MAPPING_ROUTING_KEY = "mapping_routingkey";

    public static final String START_MIGRATION_QUEUE = "start.migration";
    public static final String START_MIGRATION_ROUTING_KEY = "start_routingkey";

    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue queue() {
        return new Queue(QUEUE);
    }

    @Bean
    public Queue queue2() {
        return new Queue(START_MIGRATION_QUEUE);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(MONITORING_ROUTING_KEY);
    }

    @Bean
    public Binding binding2(Queue queue2, TopicExchange exchange) {
        return BindingBuilder.bind(queue2).to(exchange).with(START_MIGRATION_ROUTING_KEY);
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
