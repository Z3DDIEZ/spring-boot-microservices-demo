package com.meridian.inventoryservice.infrastructure;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange.orders}")
    private String ordersExchange;

    @Value("${app.rabbitmq.exchange.inventory}")
    private String inventoryExchange;

    @Value("${app.rabbitmq.queue.order-created}")
    private String orderCreatedQueue;

    @Value("${app.rabbitmq.queue.inventory-reserved}")
    private String inventoryReservedQueue;

    @Value("${app.rabbitmq.routing-key.order-created}")
    private String orderCreatedRoutingKey;

    @Value("${app.rabbitmq.routing-key.inventory-reserved}")
    private String inventoryReservedRoutingKey;

    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange(ordersExchange);
    }

    @Bean
    public TopicExchange inventoryExchange() {
        return new TopicExchange(inventoryExchange);
    }

    @Bean
    public Queue orderCreatedQueue() {
        // durable = true
        return new Queue(orderCreatedQueue, true);
    }

    @Bean
    public Queue inventoryReservedQueue() {
        return new Queue(inventoryReservedQueue, true);
    }

    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(ordersExchange).with(orderCreatedRoutingKey);
    }

    @Bean
    public Binding inventoryReservedBinding(Queue inventoryReservedQueue, TopicExchange inventoryExchange) {
        return BindingBuilder.bind(inventoryReservedQueue).to(inventoryExchange).with(inventoryReservedRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
