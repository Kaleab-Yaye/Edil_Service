package com.edil.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GetPdfInfoRequest(
       @NotNull UUID campaignId
) {
}
