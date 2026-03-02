package com.meridian.order.application;

import com.meridian.order.domain.OutboxEvent;
import com.meridian.order.infrastructure.EventPublisher;
import com.meridian.order.infrastructure.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Background worker fulfilling the "Relay" role in the Transactional Outbox
 * pattern.
 * <p>
 * This daemon continually sweeps the local database for pending Domain Events
 * that haven't yet been successfully shipped to the RabbitMQ broker.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final EventPublisher eventPublisher;

    /**
     * Executes periodically (via Spring's {@code @Scheduled}) to scrape up to 100
     * pending messages
     * from the {@code outbox_events} table.
     * <p>
     * Provides <i>At-Least-Once Delivery</i> guarantees. If the RabbitMQ broker
     * accepts the
     * message, it flips the local published flag. If the broker is unreachable, it
     * leaves the flag false
     * allowing subsequent sweeps to retry.
     */
    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : pendingEvents) {
            try {
                log.info("Publishing OutboxEvent [ID: {}, Type: {}]", event.getId(), event.getEventType());

                // Push to RabbitMQ
                eventPublisher.publishEvent(event.getEventType(), event.getPayload());

                // Flip flag to true so it isn't picked up again
                event.markPublished();
                outboxEventRepository.save(event);

            } catch (Exception e) {
                log.error("Failed to publish OutboxEvent [ID: {}]. Will retry on next tick.", event.getId(), e);
                // We break here to avoid processing subsequent events out of order,
                // or we could continue if ordering isn't strictly required.
                // For simplicity, we'll continue and let the next tick grab it.
            }
        }
    }
}
