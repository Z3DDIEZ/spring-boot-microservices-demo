package com.meridian.notificationservice.application;

import com.meridian.notificationservice.domain.Notification;
import com.meridian.notificationservice.infrastructure.db.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core Application Service governing all outbound messaging campaigns.
 * <p>
 * Implements strict fault tolerance rules. It guarantees attempts are
 * synchronously
 * recorded into local PostgreSQL before risky port traversal, natively enabling
 * later retries if the external provider is experiencing an outage.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailSender emailSender;

    /**
     * Crafts and dispatches a standardized purchase verification receipt to the
     * customer.
     * <p>
     * Implements an implicit Saga orchestration step. If the {@link EmailSender}
     * adapter
     * throws an exception, the local notification state transitions to
     * {@code FAILED},
     * and the exception is consciously rethrown downstream to force RabbitMQ's DLQ
     * rules.
     *
     * @param recipientEmail The active email string to send the receipt to.
     * @param orderId        The logical upstream order tracker referencing the
     *                       purchase.
     * @throws RuntimeException Intercepts, wraps, and rethrows adapter delivery
     *                          failures.
     */
    @Transactional
    public void sendOrderConfirmation(String recipientEmail, String orderId) {
        log.info("Processing order confirmation for order: {}", orderId);

        String subject = "Meridian Order Confirmation: " + orderId;
        String body = "Thank you for your order! Your order ID is " + orderId + ".\nWe will notify you when it ships.";

        Notification notification = Notification.builder()
                .recipientEmail(recipientEmail)
                .subject(subject)
                .body(body)
                .build();

        // 1. Save initially as PENDING
        notificationRepository.save(notification);

        try {
            // 2. Attempt delivery via the port
            emailSender.sendEmail(recipientEmail, subject, body);

            // 3. Update status on success
            notification.markAsSent();
            log.info("Order confirmation sent successfully to: {}", recipientEmail);

        } catch (Exception e) {
            // 3. Update status on failure
            notification.markAsFailed();
            log.error("Failed to send order confirmation to: {}", recipientEmail, e);

            // Re-throw so Spring AMQP knows the processing failed (for retries/DLQ)
            throw new RuntimeException("Email delivery failed", e);
        }

        // The @Transactional commit will persist the final state (SENT or FAILED)
    }
}
