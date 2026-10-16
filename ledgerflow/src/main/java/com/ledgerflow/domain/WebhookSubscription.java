package com.ledgerflow.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * A merchant's subscription to receive webhook notifications for payment events.
 * Each subscription specifies a target URL and a shared secret used for HMAC signing.
 */
@Entity
@Table(name = "webhook_subscriptions")
public class WebhookSubscription {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    /** Human-readable label, e.g. "Production endpoint". */
    @Column(nullable = false)
    private String name;

    /** HTTPS URL that receives POST requests with event payloads. */
    @Column(nullable = false)
    private String url;

    /** Shared secret used to compute the HMAC-SHA256 signature sent in the header. */
    @Column(nullable = false)
    private String secret;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WebhookSubscription() {}

    public WebhookSubscription(String name, String url, String secret) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.url = url;
        this.secret = secret;
        this.active = true;
        this.createdAt = Instant.now();
    }

    public void deactivate() { this.active = false; }
    public void activate() { this.active = true; }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getUrl() { return url; }
    public String getSecret() { return secret; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
