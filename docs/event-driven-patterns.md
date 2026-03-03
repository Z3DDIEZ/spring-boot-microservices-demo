# Event-Driven Patterns

## Saga Pattern Implementation

The Order Service orchestrates the **Create Order** Saga for distributed transactions.

**Saga: Create Order**

```
1. Reserve Inventory (compensatable)
   → Publish: InventoryReservationRequested
   ← Consume: InventoryReserved OR InventoryReservationFailed

2. Process Payment (compensatable)
   → Publish: PaymentRequested
   ← Consume: PaymentProcessed OR PaymentFailed

3. Confirm Order (non-compensatable)
   → Update order status to CONFIRMED
   → Publish: OrderConfirmed

Compensation Logic:
- If InventoryReservationFailed → Cancel Order
- If PaymentFailed → Release Inventory → Cancel Order
```

## Outbox Pattern Scheduler

To guarantee event delivery even if the Message Broker (RabbitMQ) is down, the Order Service implements the Outbox Pattern.

```java
// Runs every 1 second
@Scheduled(fixedDelay = 1000)
public void publishPendingEvents() {
    List<OutboxEvent> pending = outboxRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();

    for (OutboxEvent event : pending) {
        try {
            rabbitTemplate.convertAndSend(
                "orders.exchange",
                event.getEventType(),
                event.getPayload()
            );
            event.markPublished();
            outboxRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to publish event {}", event.getId(), e);
            // Retry on next cycle
        }
    }
}
```

> **Kubernetes Scaling Note**: In a multi-replica Kubernetes deployment of `order-service`, having multiple pods polling the same `outbox_events` table simultaneously can lead to duplicate event publishing. To make this production-ready for horizontal scaling, a distributed lock (e.g., Spring Integration Redis Lock or ShedLock) must be applied to the `@Scheduled` method to ensure only one pod executes the publisher loop at a time.

## Domain Events

These are the core domain events published by the Order Service to the `orders.exchange` (topic).

**OrderCreatedEvent**

```json
{
  "eventType": "OrderCreated",
  "orderId": "uuid",
  "userId": "uuid",
  "items": [
    {
      "productId": "uuid",
      "quantity": 2
    }
  ],
  "totalAmount": 99.98,
  "timestamp": "2026-02-26T10:00:00Z"
}
```

**OrderConfirmedEvent**

```json
{
  "eventType": "OrderConfirmed",
  "orderId": "uuid",
  "confirmedAt": "2026-02-26T10:05:00Z"
}
```

**OrderCancelledEvent**

```json
{
  "eventType": "OrderCancelled",
  "orderId": "uuid",
  "reason": "User requested cancellation",
  "cancelledAt": "2026-02-26T10:10:00Z"
}
```
