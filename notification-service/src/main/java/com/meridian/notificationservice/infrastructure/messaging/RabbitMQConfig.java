package com.meridian.notificationservice.infrastructure.messaging;

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

    public static final String EXCHANGE_ORDERS = "orders.exchange";
    public static final String QUEUE_NOTIFICATION_ORDER_CREATED = "notification.order.created.queue";
    public static final String QUEUE_NOTIFICATION_ORDER_CREATED_DLQ = "notification.order.created.queue.dlq";
    public static final String ROUTING_KEY_ORDER_CREATED = "order.created.key";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange ordersExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_ORDERS)
                .durable(true)
                .build();
    }

    @Bean
    public Queue notificationOrderCreatedQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICATION_ORDER_CREATED)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_NOTIFICATION_ORDER_CREATED_DLQ)
                .build();
    }

    @Bean
    public Queue notificationOrderCreatedDlq() {
        return QueueBuilder.durable(QUEUE_NOTIFICATION_ORDER_CREATED_DLQ).build();
    }

    @Bean
    public Binding notificationOrderCreatedBinding() {
        return BindingBuilder.bind(notificationOrderCreatedQueue())
                .to(ordersExchange())
                .with(ROUTING_KEY_ORDER_CREATED);
    }
}
