package com.meridian.inventoryservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "inventory_reservations")
public class InventoryReservation {

    @Id
    private String id;

    @Indexed
    private UUID orderId;

    @Indexed
    private String productId;

    private Integer quantity;

    private ReservationStatus status;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
    
    public static InventoryReservation create(UUID orderId, String productId, int quantity) {
        return InventoryReservation.builder()
                .orderId(orderId)
                .productId(productId)
                .quantity(quantity)
                .status(ReservationStatus.PENDING)
                .build();
    }
    
    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }
    
    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }
}
