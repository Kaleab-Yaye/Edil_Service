package com.edil.service;


import com.edil.config.util.StoreCampaignToSlotHashMap;
import com.edil.domain.Campaign;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CampaignServiceUtil {

    @Async
    public  void addCampaignToCampaignToAvailableSlotMap(Campaign campaign){
        StoreCampaignToSlotHashMap.campaignToSlotStore.put(campaign.getId(), campaign.getTargetEntries()-campaign.getJoinedUsers());
    }
}
