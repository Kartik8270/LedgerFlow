package com.ledgerflow.reconciliation;

import com.ledgerflow.domain.SettlementRecord;
import com.ledgerflow.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Parses a CSV settlement file from an acquirer. Expected format (header row required):
 *
 * <pre>
 * external_reference,payment_id,amount_minor,currency,settled_at
 * ARN-001,a3f2c1e0-...,150000,INR,2024-09-15T10:30:00Z
 * ARN-002,,50000,INR,2024-09-15T11:00:00Z
 * </pre>
 *
 * The {@code payment_id} column may be empty for transactions the acquirer cannot
 * cross-reference to our system.
 */
@Component
public class SettlementFileParser {

    public List<SettlementRecord> parse(InputStream input, String fileName) {
        List<SettlementRecord> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            String header = reader.readLine();
            if (header == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Settlement file is empty");
            }

            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.isBlank()) continue;

                String[] parts = line.split(",", -1);
                if (parts.length < 5) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "Line " + lineNum + ": expected 5 columns, got " + parts.length);
                }

                String externalRef = parts[0].trim();
                String paymentIdStr = parts[1].trim();
                long amountMinor = Long.parseLong(parts[2].trim());
                String currency = parts[3].trim();
                Instant settledAt = Instant.parse(parts[4].trim());

                UUID paymentId = paymentIdStr.isEmpty() ? null : UUID.fromString(paymentIdStr);

                records.add(new SettlementRecord(
                        fileName, externalRef, paymentId, amountMinor, currency, settledAt));
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Failed to parse settlement file: " + e.getMessage());
        }

        return records;
    }
}
