package com.meridian.notificationservice.infrastructure.messaging;

import com.meridian.shared.domain.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes messages that have failed processing multiple times and been routed
 * to the Dead Letter Queue.
 */
@Component
@Slf4j
public class OrderCreatedDlqConsumer {

    /**
     * Siphons irrevocably failed messages from the DLQ for ultimate analysis.
     * Prevents poison-pill messages from indefinitely halting core queue
     * processing.
     *
     * @param event The original event that exhausted normal delivery retries.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION_ORDER_CREATED_DLQ)
    public void processFailedMessage(OrderCreatedEvent event) {
        log.error(
                "DLQ: Received failed OrderCreatedEvent for order: {}. This requires manual intervention or an automated retry strategy.",
                event.getOrderId());

        // In a real production system, this would potentially:
        // 1. Update the Notification entity status to 'DLQ_FAILED'
        // 2. Alert operations team via PagerDuty/Slack
        // 3. Store the event payload in a separate 'failed_events' table for manual
        // replay
    }
}
