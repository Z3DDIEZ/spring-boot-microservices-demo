package com.meridian.inventoryservice.application.event;

import com.meridian.inventoryservice.application.InventoryService;
import com.meridian.shared.domain.event.OrderCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * AMQP Consumer asynchronously listening for globally broadcast "Order Created"
 * integration events.
 * <p>
 * This represents the trigger sequence for the local Inventory Saga. It
 * effectively
 * decouples the tight HTTP bindings between the Order and Inventory boundary
 * contexts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final InventoryService inventoryService;

    /**
     * Primary message handler executing upon the arrival of a serialized domain
     * event.
     * Decodes the payload and orchestrates the localized stock reservation logic.
     *
     * @param event The structured event payload containing items requiring
     *              fulfillment.
     */
    @RabbitListener(queues = "${app.rabbitmq.queue.order-created}")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for Order ID: {}", event.getOrderId());

        Map<String, Integer> itemsToReserve = event.getItems().stream()
                .collect(Collectors.toMap(
                        item -> item.getProductId().toString(), // Convert UUID to String for SKU matching (assuming the
                                                                // product repo uses UUID strings as SKU or ID)
                        OrderCreatedEvent.OrderItemPayload::getQuantity));

        try {
            inventoryService.reserveStock(event.getOrderId(), itemsToReserve);
            log.info("Stock reservation completed for Order ID: {}", event.getOrderId());

            // TODO: In a full saga, we would publish an InventoryReservedEvent here

        } catch (Exception e) {
            log.error("Failed to process stock reservation for Order ID: {}", event.getOrderId(), e);
            // TODO: In a full saga, we would publish an InventoryFailedEvent here to
            // trigger compensation
            // Note: By throwing the exception, the message will go back to the queue or DLQ
            throw e;
        }
    }
}
