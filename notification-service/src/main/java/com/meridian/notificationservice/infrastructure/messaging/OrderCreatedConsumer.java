package com.meridian.notificationservice.infrastructure.messaging;

import com.meridian.notificationservice.application.NotificationService;
import com.meridian.shared.domain.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Resides in infrastructure. Acts as a driving adapter that receives
 * external Spring AMQP messages and delegates them to the Application Use Case.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedConsumer {

    private final NotificationService notificationService;

    /**
     * Intercepts "Order Created" events globally distributed by the messaging
     * topology.
     * Immediately triggers the downstream task to send a confirmation receipt to
     * the user.
     *
     * @param event The structured event carrying payload details about the new
     *              order.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION_ORDER_CREATED)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for order: {}", event.getOrderId());

        // Mock email logic for now: retrieving actual user email usually requires
        // hitting auth-service,
        // storing email on order, OR embedding it in the domain event payload.
        // For simplicity in this demo without blocking cross-service calls, we'll dummy
        // an email if missing or expect the Order payload to eventually carry contact
        // info.

        String destinationEmail = "customer-" + event.getUserId() + "@example.com";

        notificationService.sendOrderConfirmation(destinationEmail, event.getOrderId().toString());
    }
}
