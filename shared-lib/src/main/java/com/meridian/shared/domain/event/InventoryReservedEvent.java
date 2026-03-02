package com.meridian.shared.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReservedEvent implements DomainEvent {

    @Builder.Default
    private UUID eventId = UUID.randomUUID();

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private UUID orderId;
    private UUID productId;
    private Integer quantity;
    private String reservationStatus;

    @Override
    public String getEventType() {
        return "InventoryReserved";
    }
}
