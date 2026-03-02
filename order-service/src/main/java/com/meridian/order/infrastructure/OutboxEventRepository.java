package com.meridian.order.infrastructure;

import com.meridian.order.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository managing the transactional outbox table.
 * Critical component of the outbox pattern used to decouple local database
 * transactions
 * from the asynchronous message broker.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Retrieves a batch of unprocessed outbox events that have yet to be shipped to
     * RabbitMQ.
     * <p>
     * Capped to mitigate memory issues under heavy loads. Ordered chronologically
     * to maintain
     * FIFO event streaming guarantees where possible.
     *
     * @return A list of maximum 100 unpublished outbox entities.
     */
    List<OutboxEvent> findTop100ByPublishedFalseOrderByCreatedAtAsc();
}
