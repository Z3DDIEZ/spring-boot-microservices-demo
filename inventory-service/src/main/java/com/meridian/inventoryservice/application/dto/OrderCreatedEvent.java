package com.meridian.inventoryservice.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private String eventId;
    private UUID orderId;
    private UUID userId;
    private BigDecimal totalAmount;
    private List<OrderItemDto> items;
    private Instant createdAt;
}
