package com.meridian.order.domain;

/**
 * Enumerates the rigidly defined lifecycle states of an {@link Order}.
 * <p>
 * These states dictate both frontend behaviors and backend saga-processing
 * capabilities.
 */
public enum OrderStatus {
    /**
     * The order is created but pending external verification (e.g., inventory
     * reservation).
     */
    PENDING,
    /**
     * Inventory is successfully reserved and the order is locked for fulfillment.
     */
    CONFIRMED,
    /** The order failed to verify or was aborted (e.g., insufficient stock). */
    CANCELLED,
    /** The order has been physically passed to a logistics provider. */
    SHIPPED,
    /** The order is confirmed to have successfully reached the customer. */
    DELIVERED
}
