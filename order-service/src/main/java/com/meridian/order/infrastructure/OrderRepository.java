package com.meridian.order.infrastructure;

import com.meridian.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository managing persistence and retrieval of
 * {@link Order} aggregates.
 * Automatically delegates to underlying Hibernate/PostgreSQL drivers.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Retrieves all historical orders placed by a specific user, ordered
     * chronologically
     * from newest to oldest.
     *
     * @param userId The unique UUID of the requesting principal.
     * @return A list of populated `Order` entities belonging to the user.
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
