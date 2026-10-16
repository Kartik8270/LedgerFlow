package com.ledgerflow.webhook;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Computes HMAC-SHA256 signatures for webhook payloads. The receiving endpoint
 * verifies the signature to confirm the event was sent by LedgerFlow and was
 * not tampered with in transit.
 *
 * <p>Signature header format: {@code sha256=<hex>}</p>
 */
@Component
public class WebhookSigningService {

    private static final String ALGORITHM = "HmacSHA256";

    /**
     * Computes the HMAC-SHA256 signature of the given payload using the
     * subscription's shared secret.
     *
     * @return the signature in {@code sha256=<hex>} format
     */
    public String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC signature", e);
        }
    }
}
