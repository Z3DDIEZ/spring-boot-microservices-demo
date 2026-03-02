package com.meridian.notificationservice.application;

import com.meridian.notificationservice.domain.Notification;
import com.meridian.notificationservice.infrastructure.db.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the sending of notifications and recording their state.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailSender emailSender;

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
