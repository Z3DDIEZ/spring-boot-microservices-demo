package com.meridian.notificationservice.infrastructure.db;

import com.meridian.notificationservice.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Standard Spring Data JPA contract for persisting and retrieving abstract
 * {@link Notification} transmission attempt logs from the relational database.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
}
