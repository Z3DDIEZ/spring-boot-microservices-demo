package com.meridian.analyticsservice.infrastructure.messaging;

import com.meridian.analyticsservice.application.AnalyticsService;
import com.meridian.shared.domain.event.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderCreatedConsumerTest {

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private OrderCreatedConsumer orderCreatedConsumer;

    @Test
    void handleOrderCreated_ShouldDelegateToAnalyticsService() {
        // Arrange
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .build();

        // Act
        orderCreatedConsumer.handleOrderCreated(event);

        // Assert
        verify(analyticsService).recordOrderMetric(event);
    }
}
