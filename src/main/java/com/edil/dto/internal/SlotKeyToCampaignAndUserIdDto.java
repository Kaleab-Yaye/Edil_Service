package com.edil.dto.internal;

import java.time.LocalDateTime;
import java.util.UUID;

public record SlotKeyToCampaignAndUserIdDto(
      UUID campaignId,
      String userEmail,
      LocalDateTime cachePutAt

) {
    public  static  SlotKeyToCampaignAndUserIdDto returnSlotKeyToCampaignAndUserIdDtoWithTime(UUID campaignId, String  userEmail){
        return  new SlotKeyToCampaignAndUserIdDto(campaignId, userEmail, LocalDateTime.now());
    }
}
