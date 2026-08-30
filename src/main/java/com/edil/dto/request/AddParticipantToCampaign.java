package com.edil.dto.request;

import java.util.UUID;

public record AddParticipantToCampaign(UUID campaignId, String fullPaymentLink) {

}