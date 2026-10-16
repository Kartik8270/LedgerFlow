package com.ledgerflow.api.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record CreateWebhookRequest(
        @NotBlank String name,
        @NotBlank @URL String url,
        @NotBlank String secret
) {}
