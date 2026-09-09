package com.edil.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;



public record AddUserTOCampaignByAdminRequest (
       @NotNull UUID campaignId,
        @NotNull UUID userId,
       @NotBlank String paymentLink
)



{



}
