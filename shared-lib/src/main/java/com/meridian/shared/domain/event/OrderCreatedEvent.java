package com.meridian.shared.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvent implements DomainEvent {

    @Builder.Default
    private UUID eventId = UUID.randomUUID();
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private UUID orderId;
    private UUID userId;
    private List<OrderItemPayload> items;
    private BigDecimal totalAmount;

    @Override
    public String getEventType() {
        return "OrderCreated";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemPayload {
        private UUID productId;
        private Integer quantity;
    }
}
