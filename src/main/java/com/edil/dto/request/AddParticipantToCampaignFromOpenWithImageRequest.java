package com.edil.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record AddParticipantToCampaignFromOpenWithImageRequest
        (
                @NotBlank String firstName,
                @NotBlank String lastName,
                @NotBlank
                @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
                String phoneNumber,
                @NotBlank
                String refundAccountNumber,
                @NotNull UUID campaignId,
                @NotNull UUID receiptKey

        )


{


}
