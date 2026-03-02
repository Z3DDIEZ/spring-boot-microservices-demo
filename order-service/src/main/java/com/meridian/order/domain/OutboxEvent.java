package com.meridian.order.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain entity representing an outbound analytical or operational event
 * targeted for the message broker.
 * <p>
 * This class forms the backbone of the <b>Transactional Outbox Pattern</b>.
 * Instead of engaging in a
 * distributed two-phase commit with RabbitMQ, the service persists its
 * mutations (e.g., creating an order)
 * and an {@code OutboxEvent} record inside the <i>same</i> local ACIDs
 * relational transaction.
 * A separate polling mechanism later safely transmits these events to the
 * message broker.
 */
@Entity
@Table(name = "outbox_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    /**
     * Internal primary key for the outbox tracking table.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The business identifier of the aggregate this event represents (e.g., the
     * Order ID).
     */
    @Column(nullable = false, updatable = false)
    private String aggregateId;

    /**
     * The canonical domain classification of the aggregate (e.g., "Order").
     */
    @Column(nullable = false, updatable = false)
    private String aggregateType;

    /**
     * The explicit type of the recorded event (e.g., "OrderCreatedEvent").
     */
    @Column(nullable = false, updatable = false)
    private String eventType;

    /**
     * The raw, serialized JSON payload summarizing the event data to eventually
     * publish.
     */
    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String payload;

    /**
     * Timestamp marking exactly when the local transaction finalized the event
     * creation.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Flag dictating whether the background relay scheduler has successfully
     * successfully transported
     * this event to the message broker via AMQP.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean published = false;

    /**
     * Flags this event as successfully transmitted to the broker, concluding its
     * pending state.
     */
    public void markPublished() {
        this.published = true;
    }
}
