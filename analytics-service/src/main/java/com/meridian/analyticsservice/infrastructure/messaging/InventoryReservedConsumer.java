package com.meridian.analyticsservice.infrastructure.messaging;

import com.meridian.analyticsservice.application.AnalyticsService;
import com.meridian.shared.domain.event.InventoryReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Asynchronous AMQP driving adapter capturing domain events broadcasted by the Inventory Service.
 * Serves as the continuous ingestion tributary for inventory tracking.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryReservedConsumer {

    private final AnalyticsService analyticsService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ANALYTICS_INVENTORY_RESERVED)
    public void handleInventoryReserved(InventoryReservedEvent event) {
        log.info("Received InventoryReservedEvent for analytics: orderId={}, productId={}",
                event.getOrderId(), event.getProductId());
        analyticsService.recordInventoryMetric(event);
    }
}
