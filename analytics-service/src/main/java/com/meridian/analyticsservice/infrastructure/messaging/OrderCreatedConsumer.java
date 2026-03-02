package com.meridian.analyticsservice.infrastructure.messaging;

import com.meridian.analyticsservice.application.AnalyticsService;
import com.meridian.shared.domain.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Asynchronous AMQP driving adapter capturing domain events broadcasted by the Order Service.
 * Serves as the continuous ingestion tributary for financial and sales tracking.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedConsumer {

    private final AnalyticsService analyticsService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ANALYTICS_ORDER_CREATED)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for analytics: orderId={}", event.getOrderId());
        analyticsService.recordOrderMetric(event);
    }
}
