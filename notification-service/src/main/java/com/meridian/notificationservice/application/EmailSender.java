package com.meridian.notificationservice.application;

/**
 * Architectural outbound port for decoupling the domain logic from brute-force
 * SMTP integrations.
 * <p>
 * This interface formally lives inside the application layer, mandating that
 * whatever concrete
 * adapter fulfills it (e.g., SendGrid, Mailgun, JavaMail) adheres to simple
 * textual inputs.
 */
public interface EmailSender {

    /**
     * Sends an email.
     *
     * @param to      The recipient email address.
     * @param subject The email subject.
     * @param body    The plain text or HTML body.
     */
    void sendEmail(String to, String subject, String body);
}
