package com.meridian.inventoryservice.infrastructure;

import com.meridian.inventoryservice.domain.InventoryReservation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationRepository extends MongoRepository<InventoryReservation, String> {
    
    List<InventoryReservation> findByOrderId(UUID orderId);
    
}
