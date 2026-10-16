package com.ledgerflow.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * One row from an acquirer settlement file. Each record describes a single settled
 * transaction, identified by the acquirer's own external reference. The reconciliation
 * engine matches these against our ledger to detect mismatches.
 */
@Entity
@Table(name = "settlement_records")
public class SettlementRecord {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    /** Name of the uploaded settlement file this record came from. */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /** The acquirer's reference for this transaction (e.g. ARN or UTR). */
    @Column(name = "external_reference", nullable = false)
    private String externalReference;

    /** Our payment id, if the file included a cross-reference. */
    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "settled_at", nullable = false)
    private Instant settledAt;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected SettlementRecord() {}

    public SettlementRecord(String fileName, String externalReference, UUID paymentId,
                            long amountMinor, String currency, Instant settledAt) {
        this.id = UUID.randomUUID();
        this.fileName = fileName;
        this.externalReference = externalReference;
        this.paymentId = paymentId;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.settledAt = settledAt;
        this.uploadedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getFileName() { return fileName; }
    public String getExternalReference() { return externalReference; }
    public UUID getPaymentId() { return paymentId; }
    public long getAmountMinor() { return amountMinor; }
    public String getCurrency() { return currency; }
    public Instant getSettledAt() { return settledAt; }
    public Instant getUploadedAt() { return uploadedAt; }
}
