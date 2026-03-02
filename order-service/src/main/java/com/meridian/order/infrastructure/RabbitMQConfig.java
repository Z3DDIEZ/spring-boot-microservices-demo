package com.meridian.order.infrastructure;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AMQP configuration defining the structural exchanges, queues, and bindings
 * necessary for asynchronous event distribution within the Meridian platform.
 * Specifies the underlying JSON serialization format for network transport.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${meridian.rabbitmq.exchanges.orders}")
    private String ordersExchange;

    @Value("${meridian.rabbitmq.routing-keys.order-created}")
    private String orderCreatedRoutingKey;

    /**
     * Initializes the core messaging exchange. Components will publish to this
     * TopicExchange,
     * which dynamically routes messages into relevant queues based on bounded
     * routing keys.
     *
     * @return The Spring AMQP TopicExchange declaration.
     */
    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange(ordersExchange);
    }

    /**
     * Declares the durable physical queue where "Order Created" events reside
     * until downstream microservices consume them (e.g. inventory reservation).
     *
     * @return The Spring AMQP Queue declaration.
     */
    @Bean
    public Queue orderCreatedQueue() {
        return new Queue("order.created.queue", true);
    }

    /**
     * Defines the strict topological binding that links the explicit
     * "order-created"
     * routing key traversing through the Topic Exchange directly into the physical
     * Queue.
     *
     * @param orderCreatedQueue The target queue bean.
     * @param ordersExchange    The originating broadcast exchange bean.
     * @return The immutable structural Binding.
     */
    @Bean
    public Binding bindingOrderCreated(Queue orderCreatedQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(ordersExchange).with(orderCreatedRoutingKey);
    }

    /**
     * Overrides the default native Java AMQP serializer (which requires trusted
     * classpaths)
     * with an agnostic JSON-based message converter. Essential for
     * polyglot/microservice
     * communication.
     *
     * @return The configured Jackson converter.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
