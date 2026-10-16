package com.ledgerflow.reconciliation;

import com.ledgerflow.domain.*;
import com.ledgerflow.domain.Enums.PaymentStatus;
import com.ledgerflow.orchestration.PaymentStateMachine;
import com.ledgerflow.repository.PaymentRepository;
import com.ledgerflow.repository.ReconciliationResultRepository;
import com.ledgerflow.repository.SettlementRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reconciliation engine. Given a set of settlement records (parsed from an acquirer
 * file) and the captured payments in our ledger, it produces a result for each:
 *
 * <ul>
 *   <li><strong>MATCHED</strong> — both sides agree on amount and currency</li>
 *   <li><strong>MISMATCHED</strong> — amounts differ</li>
 *   <li><strong>LEDGER_ONLY</strong> — we captured a payment but the acquirer didn't settle it</li>
 *   <li><strong>SETTLEMENT_ONLY</strong> — the acquirer lists a transaction we have no record of</li>
 * </ul>
 *
 * Matched payments are transitioned to SETTLED.
 */
@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    private final PaymentRepository payments;
    private final SettlementRecordRepository settlementRecords;
    private final ReconciliationResultRepository results;
    private final PaymentStateMachine stateMachine;

    public ReconciliationService(PaymentRepository payments,
                                 SettlementRecordRepository settlementRecords,
                                 ReconciliationResultRepository results,
                                 PaymentStateMachine stateMachine) {
        this.payments = payments;
        this.settlementRecords = settlementRecords;
        this.results = results;
        this.stateMachine = stateMachine;
    }

    /**
     * Runs a reconciliation against all settlement records from the given file.
     * Each captured payment is matched against its settlement counterpart (by payment id).
     * Returns the run id, which can be used to query the results.
     */
    @Transactional
    public UUID reconcile(String fileName) {
        UUID runId = UUID.randomUUID();
        List<SettlementRecord> records = settlementRecords.findByFileName(fileName);
        log.info("Reconciliation run {} started for file '{}' ({} settlement records)",
                runId, fileName, records.size());

        // Index settlement records by payment id for O(1) lookup.
        Map<UUID, SettlementRecord> settlementByPayment = records.stream()
                .filter(r -> r.getPaymentId() != null)
                .collect(Collectors.toMap(SettlementRecord::getPaymentId, Function.identity(),
                        (a, b) -> b)); // last wins if duplicate

        // Collect settlement records with no payment id (settlement-only).
        List<SettlementRecord> unlinked = records.stream()
                .filter(r -> r.getPaymentId() == null)
                .toList();

        // Load all captured payments that could match.
        Set<UUID> matchedPaymentIds = new HashSet<>();
        List<ReconciliationResult> batch = new ArrayList<>();

        // Match settlement records that have a payment id.
        for (Map.Entry<UUID, SettlementRecord> entry : settlementByPayment.entrySet()) {
            UUID paymentId = entry.getKey();
            SettlementRecord sr = entry.getValue();
            Optional<Payment> paymentOpt = payments.findById(paymentId);

            if (paymentOpt.isEmpty()) {
                batch.add(ReconciliationResult.settlementOnly(
                        runId, sr.getId(), sr.getAmountMinor(), sr.getCurrency()));
                continue;
            }

            Payment payment = paymentOpt.get();
            matchedPaymentIds.add(paymentId);

            if (payment.getAmountMinor() == sr.getAmountMinor()
                    && payment.getCurrency().equals(sr.getCurrency())) {
                batch.add(ReconciliationResult.matched(
                        runId, paymentId, sr.getId(), payment.getAmountMinor(), payment.getCurrency()));
                settleIfPossible(payment);
            } else {
                batch.add(ReconciliationResult.mismatched(
                        runId, paymentId, sr.getId(),
                        payment.getAmountMinor(), sr.getAmountMinor(), payment.getCurrency()));
            }
        }

        // Settlement records with no payment id are always settlement-only.
        for (SettlementRecord sr : unlinked) {
            batch.add(ReconciliationResult.settlementOnly(
                    runId, sr.getId(), sr.getAmountMinor(), sr.getCurrency()));
        }

        // Find captured payments that have no settlement record (ledger-only).
        List<Payment> capturedPayments = payments.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.CAPTURED)
                .filter(p -> !matchedPaymentIds.contains(p.getId()))
                .toList();

        for (Payment p : capturedPayments) {
            batch.add(ReconciliationResult.ledgerOnly(
                    runId, p.getId(), p.getAmountMinor(), p.getCurrency()));
        }

        results.saveAll(batch);

        long matched = batch.stream().filter(r -> r.getStatus() == Enums.ReconciliationStatus.MATCHED).count();
        long mismatched = batch.stream().filter(r -> r.getStatus() == Enums.ReconciliationStatus.MISMATCHED).count();
        long ledgerOnly = batch.stream().filter(r -> r.getStatus() == Enums.ReconciliationStatus.LEDGER_ONLY).count();
        long settlementOnly = batch.stream().filter(r -> r.getStatus() == Enums.ReconciliationStatus.SETTLEMENT_ONLY).count();

        log.info("Reconciliation run {} complete: {} matched, {} mismatched, {} ledger-only, {} settlement-only",
                runId, matched, mismatched, ledgerOnly, settlementOnly);

        return runId;
    }

    /**
     * Transitions a matched payment to SETTLED if the state machine allows it.
     */
    private void settleIfPossible(Payment payment) {
        if (stateMachine.canTransition(payment.getStatus(), PaymentStatus.SETTLED)) {
            payment.transitionTo(PaymentStatus.SETTLED);
            payments.save(payment);
            log.debug("Payment {} transitioned to SETTLED", payment.getId());
        }
    }
}
