package com.ledgerflow.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.domain.PaymentEventRecord;
import com.ledgerflow.repository.PaymentEventRepository;
import com.ledgerflow.webhook.WebhookDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes payment events off Kafka, persists them as an append-only audit log,
 * and enqueues webhook deliveries for all active subscribers. Writes are
 * idempotent on the event id, so redelivery (Kafka is at-least-once) never
 * creates duplicates — the same discipline the payment API itself uses.
 */
@Component
@ConditionalOnProperty(name = "ledgerflow.events.transport", havingValue = "kafka")
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final PaymentEventRepository repository;
    private final WebhookDeliveryService webhookService;
    private final ObjectMapper mapper;

    public PaymentEventConsumer(PaymentEventRepository repository,
                                WebhookDeliveryService webhookService,
                                ObjectMapper mapper) {
        this.repository = repository;
        this.webhookService = webhookService;
        this.mapper = mapper;
    }

    @KafkaListener(topics = KafkaPaymentEventPublisher.TOPIC, groupId = "ledgerflow-audit")
    public void onEvent(String payload) {
        try {
            PaymentEvent event = mapper.readValue(payload, PaymentEvent.class);
            if (repository.existsById(event.eventId())) {
                return; // already recorded — at-least-once delivery is fine
            }
            repository.save(new PaymentEventRecord(
                    event.eventId(), event.paymentId(), event.type(),
                    payload, event.occurredAt()));

            // Enqueue webhook deliveries for all active subscribers.
            webhookService.enqueueForAllSubscribers(event.eventId(), event.type(), payload);

        } catch (Exception e) {
            log.error("Failed to process payment event: {}", e.getMessage());
        }
    }
}
