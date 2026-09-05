package com.edil.config.util;


import com.edil.domain.Campaign;
import com.edil.domain.Slot;
import com.edil.dto.internal.SlotKeyToCampaignAndUserIdDto;
import com.edil.repository.SlotRepository;
import com.edil.service.CampaignService;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class CampaignToSlotNumberAndSlotKeyToCampaignBuilder implements CommandLineRunner {

    private  final CampaignService campaignService;
    private  final SlotRepository slotRepository;
    private  final  Cache<UUID, SlotKeyToCampaignAndUserIdDto> slotToCampaignIdCache;
    private  final Cache<String, UUID> userEmailToSlotKeyCache;
//    private  final  StoreCampaignToSlotHashMap storeCampaignToSlotHashMap;


    @Override
    public void run(String...args) throws Exception {


        // what about when a new Campaign gets aproved? then we would also have to add it from the other end this is for the cold boot up build
        for(Campaign campaign : campaignService.getAllRunningCampaigns()){
            StoreCampaignToSlotHashMap.campaignToSlotStore.put(campaign.getId(), new AtomicInteger( campaign.getTargetEntries()-campaign.getJoinedUsers()));
        }

        // now we build the Cache, The idea is that the db slot available will always be highjer than the slot in memory cache so the cache should be built later.

        for(Slot slot : slotRepository.findAll()){
            slotToCampaignIdCache.put(slot.getId(), SlotKeyToCampaignAndUserIdDto.returnSlotKeyToCampaignAndUserIdDtoWithTime (slot.getCampaignId(), slot.getUserEmail()) );

            StoreCampaignToSlotHashMap.campaignToSlotStore.get(slot.getCampaignId()).set(StoreCampaignToSlotHashMap.campaignToSlotStore.get(slot.getCampaignId()).intValue()-1);

            userEmailToSlotKeyCache.put(slot.getUserEmail(), slot.getId());


        }

    }
}

