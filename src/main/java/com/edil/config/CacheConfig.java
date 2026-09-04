package com.edil.config;


import com.edil.config.util.StoreCampaignToSlotHashMap;
import com.edil.service.CampaignParticipantServiceUtil;
import com.edil.service.CampaignService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class CacheConfig {

    private final CampaignService campaignService;
    private final CampaignParticipantServiceUtil campaignParticipantServiceUtil;



    @Bean
    Cache<UUID, UUID> slotKeyTOCampaignCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .scheduler(Scheduler.systemScheduler())
                .removalListener(this::removeListener)
                .build();
    }


    private void removeListener(UUID slotKey, UUID campaignId, RemovalCause cause) {


        // this shit also had concurency issue lol

        if(cause.wasEvicted()){
            while(true){
                int expectedValue = StoreCampaignToSlotHashMap.campaignToSlotStore.get(campaignId).intValue();

                if(StoreCampaignToSlotHashMap.campaignToSlotStore.get(campaignId).compareAndSet(expectedValue, expectedValue+1)){
                    return;
                };


            }

            // the cache was evivated the catch holder did't make the required opration in time
        }

        // if is removed manually it means a payment was made in that slot interval, so no need to updated the map
        if(campaignService.updateUserCount(campaignId)){

            campaignParticipantServiceUtil.archiveParticipantsOfAnEndedCampaign(campaignId);

        };

        }




}
