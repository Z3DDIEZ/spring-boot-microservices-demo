package com.meridian.order.infrastructure;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Component responsible for pushing outbox events to the RabbitMQ exchange.
 * <p>
 * This acts as the translation layer between the database-driven Outbox
 * Scheduler
 * and the actual AMQP driver. It maps internal event types (e.g.,
 * "OrderCreated")
 * to specific routing keys expected by downstream consumers.
 */
@Service
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String ordersExchange;

    public EventPublisher(RabbitTemplate rabbitTemplate,
            @Value("${meridian.rabbitmq.exchanges.orders}") String ordersExchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.ordersExchange = ordersExchange;
    }

    /**
     * Translates an internal outbox event string representation into an AMQP
     * message
     * and publishes it to the dynamically injected RabbitMQ Exchange.
     *
     * @param eventType The categorical type of the event (e.g., "OrderCreated").
     * @param payload   The raw JSON string payload containing event details.
     */
    public void publishEvent(String eventType, String payload) {
        String routingKey;
        // Map abstract event types to specific AMQP routing keys
        switch (eventType) {
            case "OrderCreated":
                routingKey = "order.created.key";
                break;
            default:
                routingKey = "order.default.key";
        }

        rabbitTemplate.convertAndSend(ordersExchange, routingKey, payload);
    }
}
