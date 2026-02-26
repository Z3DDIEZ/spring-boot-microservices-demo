package com.meridian.order.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public interface DomainEvent {
    UUID getEventId();
    String getEventType();
    LocalDateTime getTimestamp();
}
