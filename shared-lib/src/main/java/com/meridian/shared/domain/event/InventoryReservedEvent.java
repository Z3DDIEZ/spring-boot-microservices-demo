package com.meridian.shared.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a domain event triggered when inventory is successfully reserved
 * (or fails to be reserved) during the order fulfillment process.
 * <p>
 * This event is typically published by the {@code inventory-service} and
 * consumed
 * by the {@code order-service} (for saga orchestration) and the
 * {@code analytics-service}
 * (for metrics).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReservedEvent implements DomainEvent {

    /**
     * Unique identifier for this event instance to ensure idempotency.
     */
    @Builder.Default
    private UUID eventId = UUID.randomUUID();

    /**
     * The timestamp when the inventory reservation attempt concluded.
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * The ID of the order that requested the inventory reservation.
     */
    private UUID orderId;

    /**
     * The ID of the product for which inventory was reserved.
     */
    private UUID productId;

    /**
     * The quantity of the product that was reserved.
     */
    private Integer quantity;

    /**
     * The status of the reservation (e.g., "CONFIRMED", "FAILED", "OUT_OF_STOCK").
     */
    private String reservationStatus;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEventType() {
        return "InventoryReserved";
    }
}
