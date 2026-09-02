package com.edil.dto.response;

import java.util.UUID;

public record CreateCampaignSlotForUserResponse(
        UUID slotKey,
        boolean slotAvailable
) {
}
