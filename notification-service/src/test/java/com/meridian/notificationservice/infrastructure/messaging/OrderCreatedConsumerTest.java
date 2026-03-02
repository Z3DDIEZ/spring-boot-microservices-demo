package com.meridian.notificationservice.infrastructure.messaging;

import com.meridian.notificationservice.application.NotificationService;
import com.meridian.shared.domain.event.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderCreatedConsumerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderCreatedConsumer orderCreatedConsumer;

    @Test
    void handleOrderCreated_ShouldDelegateToNotificationService() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .build();

        // Act
        orderCreatedConsumer.handleOrderCreated(event);

        // Assert
        verify(notificationService).sendOrderConfirmation(contains(userId.toString()), eq(orderId.toString()));
    }
}
