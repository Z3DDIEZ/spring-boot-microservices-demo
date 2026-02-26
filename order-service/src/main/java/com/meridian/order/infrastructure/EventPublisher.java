package com.meridian.order.infrastructure;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String ordersExchange;

    public EventPublisher(RabbitTemplate rabbitTemplate, 
                          @Value("${meridian.rabbitmq.exchanges.orders}") String ordersExchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.ordersExchange = ordersExchange;
    }

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
