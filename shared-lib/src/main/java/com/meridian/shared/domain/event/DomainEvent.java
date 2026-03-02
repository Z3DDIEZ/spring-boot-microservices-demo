package com.meridian.shared.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A foundational interface for all Domain Events within the Meridian system.
 * Domain events represent something that has happened in the domain that is of
 * interest to other parts of the system.
 */
public interface DomainEvent {

    /**
     * Retrieves the unique identifier for this specific event instance.
     * This is crucial for idempotency and deduplication across microservices.
     *
     * @return The unique UUID of the event.
     */
    UUID getEventId();

    /**
     * Retrieves the type of the event, typically used for routing messages
     * or determining the payload structure during deserialization.
     *
     * @return A string representation of the event type (e.g., "OrderCreated").
     */
    String getEventType();

    /**
     * Retrieves the exact timestamp when this event occurred in the domain.
     *
     * @return The LocalDateTime when the event was logically raised.
     */
    LocalDateTime getTimestamp();
}
