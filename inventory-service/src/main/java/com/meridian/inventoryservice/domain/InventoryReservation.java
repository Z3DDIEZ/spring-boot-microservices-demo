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

/**
 * Domain entity representing a temporary or confirmed hold on physical
 * inventory.
 * <p>
 * This MongoDB document acts as a safety mechanism during distributed saga
 * transactions.
 * When an order is initially created, inventory is only "reserved". If the
 * payment fails
 * downstream, this reservation can be selectively cancelled to free up stock.
 */
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

    /**
     * Factory method to safely generate a new pending reservation blueprint.
     *
     * @param orderId   The UUID of the parent order dictating this hold.
     * @param productId The MongoDB ID of the physical product being held.
     * @param quantity  The discrete quantity of items to hold.
     * @return A transient {@code InventoryReservation} entity.
     */
    public static InventoryReservation create(UUID orderId, String productId, int quantity) {
        return InventoryReservation.builder()
                .orderId(orderId)
                .productId(productId)
                .quantity(quantity)
                .status(ReservationStatus.PENDING)
                .build();
    }

    /**
     * Mutates the state to conceptually lock the inventory for impending shipment.
     */
    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    /**
     * Mutates the state to abort the hold, demanding external logic (services)
     * physically restock the associated {@link Product}.
     */
    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }
}
