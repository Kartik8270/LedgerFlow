package com.ledgerflow.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * One line of a reconciliation run's output. Each result describes the match
 * outcome for a single payment or settlement record: matched, mismatched
 * (amount differs), ledger-only (no settlement record), or settlement-only
 * (no matching payment in our system).
 */
@Entity
@Table(name = "reconciliation_results")
public class ReconciliationResult {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    /** Groups all results from the same reconciliation run. */
    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "settlement_record_id")
    private UUID settlementRecordId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.ReconciliationStatus status;

    @Column(name = "ledger_amount_minor")
    private Long ledgerAmountMinor;

    @Column(name = "settlement_amount_minor")
    private Long settlementAmountMinor;

    @Column(length = 3)
    private String currency;

    @Column(name = "discrepancy_minor")
    private Long discrepancyMinor;

    @Column
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReconciliationResult() {}

    private ReconciliationResult(UUID runId, UUID paymentId, UUID settlementRecordId,
                                 Enums.ReconciliationStatus status,
                                 Long ledgerAmount, Long settlementAmount,
                                 String currency, String note) {
        this.id = UUID.randomUUID();
        this.runId = runId;
        this.paymentId = paymentId;
        this.settlementRecordId = settlementRecordId;
        this.status = status;
        this.ledgerAmountMinor = ledgerAmount;
        this.settlementAmountMinor = settlementAmount;
        this.currency = currency;
        this.discrepancyMinor = (ledgerAmount != null && settlementAmount != null)
                ? ledgerAmount - settlementAmount : null;
        this.note = note;
        this.createdAt = Instant.now();
    }

    public static ReconciliationResult matched(UUID runId, UUID paymentId, UUID settlementId,
                                               long amount, String currency) {
        return new ReconciliationResult(runId, paymentId, settlementId,
                Enums.ReconciliationStatus.MATCHED, amount, amount, currency, null);
    }

    public static ReconciliationResult mismatched(UUID runId, UUID paymentId, UUID settlementId,
                                                  long ledgerAmount, long settlementAmount,
                                                  String currency) {
        return new ReconciliationResult(runId, paymentId, settlementId,
                Enums.ReconciliationStatus.MISMATCHED, ledgerAmount, settlementAmount, currency,
                "Amount discrepancy: " + (ledgerAmount - settlementAmount) + " minor units");
    }

    public static ReconciliationResult ledgerOnly(UUID runId, UUID paymentId,
                                                  long amount, String currency) {
        return new ReconciliationResult(runId, paymentId, null,
                Enums.ReconciliationStatus.LEDGER_ONLY, amount, null, currency,
                "No matching settlement record found");
    }

    public static ReconciliationResult settlementOnly(UUID runId, UUID settlementId,
                                                      long amount, String currency) {
        return new ReconciliationResult(runId, null, settlementId,
                Enums.ReconciliationStatus.SETTLEMENT_ONLY, null, amount, currency,
                "No matching payment in ledger");
    }

    public UUID getId() { return id; }
    public UUID getRunId() { return runId; }
    public UUID getPaymentId() { return paymentId; }
    public UUID getSettlementRecordId() { return settlementRecordId; }
    public Enums.ReconciliationStatus getStatus() { return status; }
    public Long getLedgerAmountMinor() { return ledgerAmountMinor; }
    public Long getSettlementAmountMinor() { return settlementAmountMinor; }
    public String getCurrency() { return currency; }
    public Long getDiscrepancyMinor() { return discrepancyMinor; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}
