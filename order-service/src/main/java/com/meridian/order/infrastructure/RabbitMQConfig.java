package com.meridian.order.infrastructure;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${meridian.rabbitmq.exchanges.orders}")
    private String ordersExchange;

    @Value("${meridian.rabbitmq.routing-keys.order-created}")
    private String orderCreatedRoutingKey;

    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange(ordersExchange);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return new Queue("order.created.queue", true);
    }

    @Bean
    public Binding bindingOrderCreated(Queue orderCreatedQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(ordersExchange).with(orderCreatedRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
