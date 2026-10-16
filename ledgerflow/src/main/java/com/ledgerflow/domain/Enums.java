package com.ledgerflow.domain;

public final class Enums {
    private Enums() {}

    /** Normal accounting classification. Assets increase on DEBIT; liabilities increase on CREDIT. */
    public enum AccountType { ASSET, LIABILITY, REVENUE, EXPENSE }

    /** The two sides of every ledger line. */
    public enum Direction { DEBIT, CREDIT }

    /**
     * Lifecycle of a payment. Phase 2 adds SETTLED as the terminal happy-path
     * state after settlement files confirm funds arrived. Valid transitions are
     * enforced by {@link com.ledgerflow.orchestration.PaymentStateMachine}.
     *
     * <pre>
     *   INITIATED ──► AUTHORIZED ──► CAPTURED ──► SETTLED
     *       │              │             │
     *       └──► FAILED    └──► FAILED   ├──► REFUNDED
     *                                    └──► FAILED
     * </pre>
     */
    public enum PaymentStatus {
        INITIATED, AUTHORIZED, CAPTURED, SETTLED, FAILED, REFUNDED
    }

    /** Webhook delivery status. */
    public enum DeliveryStatus {
        PENDING, DELIVERED, FAILED, DEAD_LETTERED
    }

    /** Reconciliation match outcome. */
    public enum ReconciliationStatus {
        MATCHED, MISMATCHED, LEDGER_ONLY, SETTLEMENT_ONLY
    }
}
