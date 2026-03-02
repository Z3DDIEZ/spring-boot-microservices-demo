package com.meridian.notificationservice.infrastructure.mail;

import com.meridian.notificationservice.application.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Concrete implementation of the EmailSender port using Spring's JavaMailSender.
 * Resides in infrastructure as it handles external integration.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailSenderImpl implements EmailSender {

    private final JavaMailSender javaMailSender;

    @Override
    public void sendEmail(String to, String subject, String body) {
        log.debug("Preparing email to: {}, Subject: {}", to, subject);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@meridian.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        javaMailSender.send(message);
    }
}
