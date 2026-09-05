package com.edil.dto.response;

import java.util.UUID;

public record CanParticipantJoinCampaignResponse(
        boolean canJoin,
        boolean hasReservedSlot,
        UUID campaignId,
        UUID slotKey
) {
}
