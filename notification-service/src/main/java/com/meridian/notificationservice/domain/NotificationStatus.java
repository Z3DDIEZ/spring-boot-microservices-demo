package com.meridian.notificationservice.domain;

/**
 * Discrete lifecycle states tracking the physical delivery of an outbound
 * message.
 */
public enum NotificationStatus {
    /** Staged for processing but not yet relayed to the provider. */
    PENDING,
    /** Successfully accepted by the external provider (e.g. SMTP server). */
    SENT,
    /** Irrecoverable delivery failure (e.g. malformed address, hard bounce). */
    FAILED
}
