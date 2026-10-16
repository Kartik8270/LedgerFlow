package com.ledgerflow.api.dto;

import com.ledgerflow.domain.WebhookSubscription;

import java.time.Instant;
import java.util.UUID;

public record WebhookSubscriptionResponse(
        UUID id,
        String name,
        String url,
        boolean active,
        Instant createdAt
) {
    public static WebhookSubscriptionResponse from(WebhookSubscription s) {
        return new WebhookSubscriptionResponse(
                s.getId(), s.getName(), s.getUrl(), s.isActive(), s.getCreatedAt());
    }
}
