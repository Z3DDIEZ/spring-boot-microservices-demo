package com.meridian.inventoryservice.domain;

/**
 * Finite state machine outlining the lifecycle of an
 * {@link InventoryReservation}.
 */
public enum ReservationStatus {
    /** A tentative hold awaiting downstream verification. */
    PENDING,
    /**
     * Irrevocably locked state, goods are scheduled for physical outbound
     * fulfillment.
     */
    CONFIRMED,
    /**
     * The hold was aborted and stock was remitted back to the primary
     * {@link Product}.
     */
    CANCELLED
}
