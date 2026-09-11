package com.edil.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record AddParticipantToCampaignFromOpenWithLinkRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
        String phoneNumber,
        @NotBlank
        String refundAccountNumber,
        @NotNull UUID campaignId,
        @NotBlank
        @Pattern(regexp = "^https://mbreciept\\.cbe\\.com\\.et/v2-([a-zA-Z0-9]+)$", message = "link must be a valid cbe link")
        String paymentLink



)
{
    public static AddParticipantToCampaignFromOpenWithLinkRequest returnMeFromImageReceiptRequest(AddParticipantToCampaignFromOpenWithImageRequest addRequest, String paymentLink){
        return new AddParticipantToCampaignFromOpenWithLinkRequest(
                addRequest.firstName(),
                addRequest.lastName(),
                addRequest.phoneNumber(),
                addRequest.refundAccountNumber(),
                addRequest.campaignId(),
                paymentLink

        );
    }
}
