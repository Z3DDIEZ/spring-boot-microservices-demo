package com.meridian.order.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents an individual line item contained within an {@link Order}.
 * <p>
 * This entity is subordinate to the Order aggregate root. It tracks the
 * specific
 * product, quantity, and snapshotted unit price at the time of purchase.
 */
@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    /**
     * The internal primary key representing this specific line item record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The parent {@link Order} aggregate root controlling this item.
     * Ignored during JSON serialization to prevent infinite recursion.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Order order;

    /**
     * The unique identifier of the physical product being purchased.
     * Logically references a product ID within the inventory/catalog domain.
     */
    @Column(nullable = false)
    private UUID productId;

    /**
     * The discrete quantity of the product requested.
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * The unit price of the product at the exact moment the order was placed.
     * Kept mathematically immutable against future product price fluctuations.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
}
