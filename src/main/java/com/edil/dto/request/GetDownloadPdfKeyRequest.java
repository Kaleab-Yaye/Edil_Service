package com.edil.dto.request;


import jakarta.validation.constraints.NotNull;
import org.checkerframework.checker.units.qual.UnknownUnits;

import java.util.UUID;

public record GetDownloadPdfKeyRequest(
       @NotNull UUID campaignId
) {
}
