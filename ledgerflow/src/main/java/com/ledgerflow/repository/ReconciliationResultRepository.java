package com.ledgerflow.repository;

import com.ledgerflow.domain.Enums;
import com.ledgerflow.domain.ReconciliationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReconciliationResultRepository extends JpaRepository<ReconciliationResult, UUID> {

    List<ReconciliationResult> findByRunId(UUID runId);

    List<ReconciliationResult> findByRunIdAndStatus(UUID runId, Enums.ReconciliationStatus status);

    long countByRunIdAndStatus(UUID runId, Enums.ReconciliationStatus status);
}
