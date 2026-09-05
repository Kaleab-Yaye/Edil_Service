package com.edil.dto.internal;

import java.util.UUID;

public record SlotKeyToCampaignAndUserIdDto(
      UUID campaignId,
      String UserEmail

) {
}
