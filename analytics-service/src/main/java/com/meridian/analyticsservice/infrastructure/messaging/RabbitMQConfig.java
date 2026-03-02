package com.meridian.analyticsservice.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchanges (must match publishers in order-service and inventory-service)
    public static final String EXCHANGE_ORDERS = "orders.exchange";
    public static final String EXCHANGE_INVENTORY = "inventory.exchange";

    // Analytics-specific queues
    public static final String QUEUE_ANALYTICS_ORDER_CREATED = "analytics.order.created.queue";
    public static final String QUEUE_ANALYTICS_ORDER_CREATED_DLQ = "analytics.order.created.queue.dlq";
    public static final String QUEUE_ANALYTICS_INVENTORY_RESERVED = "analytics.inventory.reserved.queue";
    public static final String QUEUE_ANALYTICS_INVENTORY_RESERVED_DLQ = "analytics.inventory.reserved.queue.dlq";

    // Routing keys
    public static final String ROUTING_KEY_ORDER_CREATED = "order.created.key";
    public static final String ROUTING_KEY_INVENTORY_RESERVED = "inventory.reserved.key";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // --- Exchanges ---

    @Bean
    public TopicExchange ordersExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_ORDERS).durable(true).build();
    }

    @Bean
    public TopicExchange inventoryExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_INVENTORY).durable(true).build();
    }

    // --- Order Created Queues & Binding ---

    @Bean
    public Queue analyticsOrderCreatedQueue() {
        return QueueBuilder.durable(QUEUE_ANALYTICS_ORDER_CREATED)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_ANALYTICS_ORDER_CREATED_DLQ)
                .build();
    }

    @Bean
    public Queue analyticsOrderCreatedDlq() {
        return QueueBuilder.durable(QUEUE_ANALYTICS_ORDER_CREATED_DLQ).build();
    }

    @Bean
    public Binding analyticsOrderCreatedBinding() {
        return BindingBuilder.bind(analyticsOrderCreatedQueue())
                .to(ordersExchange())
                .with(ROUTING_KEY_ORDER_CREATED);
    }

    // --- Inventory Reserved Queues & Binding ---

    @Bean
    public Queue analyticsInventoryReservedQueue() {
        return QueueBuilder.durable(QUEUE_ANALYTICS_INVENTORY_RESERVED)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_ANALYTICS_INVENTORY_RESERVED_DLQ)
                .build();
    }

    @Bean
    public Queue analyticsInventoryReservedDlq() {
        return QueueBuilder.durable(QUEUE_ANALYTICS_INVENTORY_RESERVED_DLQ).build();
    }

    @Bean
    public Binding analyticsInventoryReservedBinding() {
        return BindingBuilder.bind(analyticsInventoryReservedQueue())
                .to(inventoryExchange())
                .with(ROUTING_KEY_INVENTORY_RESERVED);
    }
}
