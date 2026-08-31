package com.edil.dto.request;

import java.util.UUID;

public record AddParticipantToCampaignRequest(UUID campaignId, String fullPaymentLink) {

}