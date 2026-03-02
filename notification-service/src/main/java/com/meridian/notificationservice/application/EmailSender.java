package com.meridian.notificationservice.application;

/**
 * Port interface for the external Email delivery mechanism.
 * This belongs in the application layer to keep business logic agnostic of SMTP/providers.
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
