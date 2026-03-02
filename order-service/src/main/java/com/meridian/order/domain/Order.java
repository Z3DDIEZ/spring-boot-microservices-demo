package com.meridian.order.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The core domain aggregate root representing an e-commerce Order.
 * <p>
 * This entity encapsulates the state and lifecycle of a customer order. Due to
 * the distributed nature of the microservices architecture, its state
 * transitions
 * (e.g., from PENDING to CONFIRMED or CANCELLED) are heavily influenced by
 * asynchronous events arriving from inventory or payment domains.
 * <p>
 * Implements optimistic locking via a {@code @Version} field to protect against
 * concurrent modifications.
 */
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    /**
     * The system-generated unique identifier for the Order.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The unique identifier of the user who placed the order.
     * Tied logically to the {@code auth-service} boundaries but unconstrained here.
     */
    @Column(nullable = false)
    private UUID userId;

    /**
     * The current lifecycle status of the order. Defaults to
     * {@link OrderStatus#PENDING}.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    /**
     * The calculated absolute total monetary amount of the order.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * The collection of individual line items within this order.
     * Managed completely by this aggregate root.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    /**
     * Timestamp representing when the order was first recorded.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp representing the most recent mutation of the order.
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * JPA optimistic locking version column. Prevents lost updates during
     * concurrent edits.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    /**
     * A domain helper method to append an {@link OrderItem} and safely maintain the
     * bidirectional persistent relationship.
     *
     * @param item The line item to add to the order.
     */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}
