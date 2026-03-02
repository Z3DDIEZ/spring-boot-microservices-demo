package com.meridian.shared.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a domain event triggered immediately after a new order is
 * successfully
 * created and saved in the {@code order-service} database.
 * <p>
 * This is a highly central event consumed by multiple downstream services,
 * including:
 * <ul>
 * <li>{@code inventory-service}: To reserve physical stock.</li>
 * <li>{@code notification-service}: To dispatch confirmation emails.</li>
 * <li>{@code analytics-service}: To aggregate financial and volume
 * metrics.</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvent implements DomainEvent {

    /**
     * Unique identifier for this event instance to ensure idempotency.
     */
    @Builder.Default
    private UUID eventId = UUID.randomUUID();

    /**
     * The timestamp when the order was logically created in the domain.
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * The unique identifier of the newly created order.
     */
    private UUID orderId;

    /**
     * The unique identifier of the user who placed the order.
     */
    private UUID userId;

    /**
     * A list containing the specific products and quantities requested in the
     * order.
     */
    private List<OrderItemPayload> items;

    /**
     * The total financial value of the order.
     */
    private BigDecimal totalAmount;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEventType() {
        return "OrderCreated";
    }

    /**
     * A simplified payload representing a line item within the
     * {@link OrderCreatedEvent}.
     * Decoupled from the internal Order entity to prevent domain leakage.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemPayload {

        /**
         * The unique identifier of the product.
         */
        private UUID productId;

        /**
         * The requested quantity of the product.
         */
        private Integer quantity;
    }
}
