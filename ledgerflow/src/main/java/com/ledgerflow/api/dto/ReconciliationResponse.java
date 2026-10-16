package com.ledgerflow.api.dto;

import com.ledgerflow.domain.ReconciliationResult;

import java.util.List;
import java.util.UUID;

public record ReconciliationResponse(
        UUID runId,
        String fileName,
        long totalRecords,
        long matched,
        long mismatched,
        long ledgerOnly,
        long settlementOnly,
        List<ResultLine> results
) {
    public record ResultLine(
            UUID paymentId,
            UUID settlementRecordId,
            String status,
            Long ledgerAmountMinor,
            Long settlementAmountMinor,
            Long discrepancyMinor,
            String note
    ) {
        public static ResultLine from(ReconciliationResult r) {
            return new ResultLine(
                    r.getPaymentId(), r.getSettlementRecordId(),
                    r.getStatus().name(), r.getLedgerAmountMinor(),
                    r.getSettlementAmountMinor(), r.getDiscrepancyMinor(), r.getNote());
        }
    }
}
