package com.meridian.inventoryservice.infrastructure;

import com.meridian.inventoryservice.domain.InventoryReservation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data MongoDB Repository managing localized stock reserves.
 * Heavily utilized during standard Order -> Inventory saga flow execution.
 */
@Repository
public interface ReservationRepository extends MongoRepository<InventoryReservation, String> {
    /**
     * Determines if a specific Order ID has already successfully reserved inventory
     * inside this node.
     * Prevents fatal double-charging during non-idempotent event replays.
     *
     * @param orderId The parent UUID.
     * @return A list of attached reservation records if found.
     */
    List<InventoryReservation> findByOrderId(UUID orderId);
}
