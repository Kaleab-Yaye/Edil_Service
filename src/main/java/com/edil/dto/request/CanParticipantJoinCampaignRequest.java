package com.edil.dto.request;

import java.util.UUID;

public record CanParticipantJoinCampaignRequest(
        UUID campaignId
) {
}
