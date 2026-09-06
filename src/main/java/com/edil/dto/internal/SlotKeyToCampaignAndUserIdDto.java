package com.edil.dto.internal;

import java.time.LocalDateTime;
import java.util.UUID;

public record SlotKeyToCampaignAndUserIdDto(
      UUID campaignId,
      String userEmail,
      LocalDateTime cachePutAt,
      boolean flaggedForCanceledSlot

) {
    public  static  SlotKeyToCampaignAndUserIdDto returnSlotKeyToCampaignAndUserIdDtoWithTime(UUID campaignId, String  userEmail){
        return  new SlotKeyToCampaignAndUserIdDto(campaignId, userEmail, LocalDateTime.now().plusMinutes(5), false);
    }

    public SlotKeyToCampaignAndUserIdDto returnSlotKeyToCampaignAndUserIdDtoFlaggedAsReplaced(){
        return new SlotKeyToCampaignAndUserIdDto(this.campaignId, this.userEmail, this.cachePutAt, true);
    }
}
