package com.ledgerflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for settlement file upload and reconciliation.
 * Creates a payment via the API, then uploads a matching settlement file and
 * verifies the reconciliation results.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ReconciliationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("ledgerflow.events.transport", () -> "noop");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void reconcileMatchingSettlement() throws Exception {
        // 1. Create a payment (uses the Phase 1 direct-capture flow).
        String paymentJson = mvc.perform(post("/v1/payments")
                        .header("Idempotency-Key", "recon-test-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"merchantReference":"recon-order-1","amountMinor":250000,"currency":"INR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("CAPTURED")))
                .andReturn().getResponse().getContentAsString();

        String paymentId = mapper.readTree(paymentJson).get("id").asText();

        // 2. Upload a settlement file that matches this payment exactly.
        String csv = "external_reference,payment_id,amount_minor,currency,settled_at\n"
                + "ARN-001," + paymentId + ",250000,INR,2024-09-15T10:30:00Z\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "settlement-2024-09-15.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        String reconJson = mvc.perform(multipart("/v1/reconciliation/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matched", is(1)))
                .andExpect(jsonPath("$.mismatched", is(0)))
                .andExpect(jsonPath("$.settlementOnly", is(0)))
                .andReturn().getResponse().getContentAsString();

        // 3. The matched payment should now be SETTLED.
        mvc.perform(get("/v1/payments/" + paymentId))
                .andExpect(jsonPath("$.status", is("SETTLED")));

        // 4. The run results should be retrievable.
        String runId = mapper.readTree(reconJson).get("runId").asText();
        mvc.perform(get("/v1/reconciliation/runs/" + runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").exists());
    }

    @Test
    void reconcileMismatchedAmount() throws Exception {
        // Create a payment for 300000 paise.
        String paymentJson = mvc.perform(post("/v1/payments")
                        .header("Idempotency-Key", "recon-test-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"merchantReference":"recon-order-2","amountMinor":300000,"currency":"INR"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String paymentId = mapper.readTree(paymentJson).get("id").asText();

        // Settlement file says 310000 — a mismatch of 10000 paise.
        String csv = "external_reference,payment_id,amount_minor,currency,settled_at\n"
                + "ARN-002," + paymentId + ",310000,INR,2024-09-15T11:00:00Z\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "settlement-mismatch.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/v1/reconciliation/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mismatched", is(1)))
                .andExpect(jsonPath("$.matched", is(0)))
                .andExpect(jsonPath("$.results[0].discrepancyMinor", is(-10000)));

        // Payment should still be CAPTURED (not settled) since amounts don't match.
        mvc.perform(get("/v1/payments/" + paymentId))
                .andExpect(jsonPath("$.status", is("CAPTURED")));
    }

    @Test
    void settlementOnlyRecord() throws Exception {
        // Upload a settlement file referencing a payment id that doesn't exist.
        String csv = "external_reference,payment_id,amount_minor,currency,settled_at\n"
                + "ARN-003,99999999-9999-9999-9999-999999999999,100000,INR,2024-09-15T12:00:00Z\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "settlement-orphan.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/v1/reconciliation/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.settlementOnly", is(1)))
                .andExpect(jsonPath("$.matched", is(0)));
    }
}
