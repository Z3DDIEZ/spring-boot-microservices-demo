package com.meridian.analyticsservice.infrastructure.messaging;

import com.meridian.analyticsservice.application.AnalyticsService;
import com.meridian.shared.domain.event.InventoryReservedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventoryReservedConsumerTest {

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private InventoryReservedConsumer inventoryReservedConsumer;

    @Test
    void handleInventoryReserved_ShouldDelegateToAnalyticsService() {
        // Arrange
        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .orderId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .quantity(3)
                .reservationStatus("CONFIRMED")
                .build();

        // Act
        inventoryReservedConsumer.handleInventoryReserved(event);

        // Assert
        verify(analyticsService).recordInventoryMetric(event);
    }
}
