package com.edil.config;


import com.edil.config.util.StoreCampaignToSlotHashMap;
import com.edil.dto.internal.SlotKeyToCampaignAndUserIdDto;
import com.edil.exception.AccountNotFoundException;
import com.edil.repository.SlotRepository;
import com.edil.service.CampaignParticipantServiceUtil;
import com.edil.service.CampaignService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;

import java.time.Duration;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class CacheConfig {

    private final CampaignService campaignService;
    private final CampaignParticipantServiceUtil campaignParticipantServiceUtil;
    private final SlotRepository slotRepository;



    @Bean
    Cache<UUID, SlotKeyToCampaignAndUserIdDto> slotKeyTOCampaignCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .scheduler(Scheduler.systemScheduler())
                .removalListener(this::removeListenerForSlotKeyToCampaignCache)
                .build();
    }


    private void removeListenerForSlotKeyToCampaignCache(UUID slotKey, SlotKeyToCampaignAndUserIdDto slotKeyToCampaignAndUserIdDto, RemovalCause cause) {

        if(slotKeyToCampaignAndUserIdDto.flaggedForCanceledSlot()){
            return;
        }


        UUID campaignId = slotKeyToCampaignAndUserIdDto.campaignId();
        userIdTOExistingSlotPresentCheck().invalidate(slotKeyToCampaignAndUserIdDto.userEmail());
        slotRepository.delete(slotRepository.findById(slotKey).orElseThrow(() -> new AccountNotFoundException("well not account not find exception but the slot wtih the follwoing id doesn exist " + slotKey)));

        if(cause.equals(RemovalCause.EXPLICIT)){

            if (campaignService.updateUserCount(campaignId)) {

                   campaignParticipantServiceUtil.archiveParticipantsOfAnEndedCampaign(campaignId);

            }
        }

        else{

            while (true) {
                int expectedValue = StoreCampaignToSlotHashMap.campaignToSlotStore.get(campaignId).intValue();

                if (StoreCampaignToSlotHashMap.campaignToSlotStore.get(campaignId).compareAndSet(expectedValue, expectedValue + 1)) {
                    // this was the isseu why the slot existed long after the cahfe is  exited
                    break;
                }
            }

        }


    }

    @Bean
    Cache<String, UUID> userIdTOExistingSlotPresentCheck() { //USER EMAIL , SLOTKEY
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .scheduler(Scheduler.systemScheduler())
                .build();
    }




}
