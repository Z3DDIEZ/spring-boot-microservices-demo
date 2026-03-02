package com.meridian.notificationservice.application;

import com.meridian.notificationservice.domain.Notification;
import com.meridian.notificationservice.infrastructure.db.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailSender emailSender;

    @InjectMocks
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    private String recipientEmail;
    private String orderId;

    @BeforeEach
    void setUp() {
        recipientEmail = "test@example.com";
        orderId = UUID.randomUUID().toString();
    }

    @Test
    void sendOrderConfirmation_Success() {
        // Arrange
        doNothing().when(emailSender).sendEmail(anyString(), anyString(), anyString());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        notificationService.sendOrderConfirmation(recipientEmail, orderId);

        // Assert
        verify(notificationRepository).save(notificationCaptor.capture());
        verify(emailSender).sendEmail(eq(recipientEmail), contains(orderId), contains(orderId));

        Notification saved = notificationCaptor.getValue();
        assertThat(saved.getRecipientEmail()).isEqualTo(recipientEmail);
        assertThat(saved.getStatus().name()).isEqualTo("SENT");
    }

    @Test
    void sendOrderConfirmation_EmailSenderThrowsException() {
        // Arrange
        doThrow(new RuntimeException("SMTP Server Down"))
                .when(emailSender).sendEmail(anyString(), anyString(), anyString());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> notificationService.sendOrderConfirmation(recipientEmail, orderId));

        assertThat(exception.getMessage()).isEqualTo("Email delivery failed");

        verify(notificationRepository).save(notificationCaptor.capture());
        Notification saved = notificationCaptor.getValue();

        assertThat(saved.getRecipientEmail()).isEqualTo(recipientEmail);
        assertThat(saved.getStatus().name()).isEqualTo("FAILED");
    }
}
