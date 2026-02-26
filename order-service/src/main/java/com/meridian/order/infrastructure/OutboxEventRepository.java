package com.meridian.order.infrastructure;

import com.meridian.order.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    
    // Finds up to 100 unpublished events, ordered by oldest first
    List<OutboxEvent> findTop100ByPublishedFalseOrderByCreatedAtAsc();
}
