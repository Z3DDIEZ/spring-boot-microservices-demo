package com.meridian.notificationservice.infrastructure.mail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailSenderImplTest {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private EmailSenderImpl emailSender;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    @Test
    void sendEmail_ShouldConstructAndSendSimpleMailMessage() {
        // Arrange
        String to = "user@example.com";
        String subject = "Test Subject";
        String body = "Test Body";

        // Act
        emailSender.sendEmail(to, subject, body);

        // Assert
        verify(javaMailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();

        assertThat(message.getTo()).containsExactly(to);
        assertThat(message.getSubject()).isEqualTo(subject);
        assertThat(message.getText()).isEqualTo(body);
        assertThat(message.getFrom()).isEqualTo("noreply@meridian.com");
    }
}
