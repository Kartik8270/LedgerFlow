package com.ledgerflow.api;

import com.ledgerflow.api.dto.ReconciliationResponse;
import com.ledgerflow.domain.Enums;
import com.ledgerflow.domain.ReconciliationResult;
import com.ledgerflow.domain.SettlementRecord;
import com.ledgerflow.exception.ApiException;
import com.ledgerflow.reconciliation.ReconciliationService;
import com.ledgerflow.reconciliation.SettlementFileParser;
import com.ledgerflow.repository.ReconciliationResultRepository;
import com.ledgerflow.repository.SettlementRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/reconciliation")
public class ReconciliationController {

    private final SettlementFileParser parser;
    private final SettlementRecordRepository settlementRecords;
    private final ReconciliationService reconciliationService;
    private final ReconciliationResultRepository results;

    public ReconciliationController(SettlementFileParser parser,
                                    SettlementRecordRepository settlementRecords,
                                    ReconciliationService reconciliationService,
                                    ReconciliationResultRepository results) {
        this.parser = parser;
        this.settlementRecords = settlementRecords;
        this.reconciliationService = reconciliationService;
        this.results = results;
    }

    /**
     * Upload a settlement file and run reconciliation against the ledger.
     * The file is parsed, records are persisted, and matching is performed
     * in a single transaction.
     */
    @PostMapping("/upload")
    public ResponseEntity<ReconciliationResponse> uploadAndReconcile(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Settlement file is empty");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            fileName = "settlement-" + UUID.randomUUID() + ".csv";
        }

        try {
            List<SettlementRecord> records = parser.parse(file.getInputStream(), fileName);
            settlementRecords.saveAll(records);

            UUID runId = reconciliationService.reconcile(fileName);
            ReconciliationResponse response = buildResponse(runId, fileName);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Reconciliation failed: " + e.getMessage());
        }
    }

    /**
     * Retrieve the results of a previous reconciliation run.
     */
    @GetMapping("/runs/{runId}")
    public ReconciliationResponse getRunResults(@PathVariable UUID runId) {
        List<ReconciliationResult> runResults = results.findByRunId(runId);
        if (runResults.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "No reconciliation run found with id: " + runId);
        }
        return buildResponse(runId, "");
    }

    private ReconciliationResponse buildResponse(UUID runId, String fileName) {
        List<ReconciliationResult> runResults = results.findByRunId(runId);

        long matched = runResults.stream()
                .filter(r -> r.getStatus() == Enums.ReconciliationStatus.MATCHED).count();
        long mismatched = runResults.stream()
                .filter(r -> r.getStatus() == Enums.ReconciliationStatus.MISMATCHED).count();
        long ledgerOnly = runResults.stream()
                .filter(r -> r.getStatus() == Enums.ReconciliationStatus.LEDGER_ONLY).count();
        long settlementOnly = runResults.stream()
                .filter(r -> r.getStatus() == Enums.ReconciliationStatus.SETTLEMENT_ONLY).count();

        List<ReconciliationResponse.ResultLine> lines = runResults.stream()
                .map(ReconciliationResponse.ResultLine::from)
                .toList();

        return new ReconciliationResponse(
                runId, fileName, runResults.size(),
                matched, mismatched, ledgerOnly, settlementOnly, lines);
    }
}
