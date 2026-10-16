package com.ledgerflow.repository;

import com.ledgerflow.domain.SettlementRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SettlementRecordRepository extends JpaRepository<SettlementRecord, UUID> {

    List<SettlementRecord> findByFileName(String fileName);

    List<SettlementRecord> findByPaymentId(UUID paymentId);
}
