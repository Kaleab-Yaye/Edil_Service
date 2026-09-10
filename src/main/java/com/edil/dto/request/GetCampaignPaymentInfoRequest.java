package com.edil.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GetCampaignPaymentInfoRequest (
        @NotNull UUID campaignId
)
{
}
