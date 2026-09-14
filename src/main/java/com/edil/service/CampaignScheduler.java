package com.edil.service;

import com.edil.domain.Campaign;
import com.edil.domain.enums.CampaignStatus;
import com.edil.dto.response.CampaignResponse;
import com.edil.repository.CampaignRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor

public class CampaignScheduler {

    private final CampaignService campaignService;
    private  final CampaignRepository campaignRepository;
    private final CreatorCampaignServiceAsyncEntry creatorCampaignServiceAsyncEntry;

    public final AtomicInteger  OpenSlots = new AtomicInteger(100);

    @Scheduled(cron = "0 * * * * *")
    public void archiveExpiredCampaignsAndGeneratePdf() {

        if (OpenSlots.intValue()==0){
            return;
        }

        log.info("well archivale started lol");

        int availableSlot = OpenSlots.intValue();
        OpenSlots.addAndGet(-1*availableSlot);

        //now we fetch the list of campaigns with the amount we want
        Pageable pageable = Pageable.ofSize(availableSlot);


        List<Campaign> campaignsBeingProcessed = campaignService.getPageableByStatusAndTime(CampaignStatus.BEING_PROCESSED, LocalDateTime.now().minusMinutes(20), pageable);

         availableSlot -= campaignsBeingProcessed.size();

         pageable = Pageable.ofSize(availableSlot);

        List<Campaign> endedCampaigns = campaignService.getPageableByStatus(CampaignStatus.ENDED_BY_CREATOR, pageable );

        availableSlot -= endedCampaigns.size();

        OpenSlots.addAndGet(availableSlot);

        List<Campaign> campaigns = new ArrayList<>(campaignsBeingProcessed);
        campaigns.addAll(endedCampaigns);
        log.info("we have got a campaign of the number: {}", campaigns.size());

        for(Campaign campaign: campaigns){
            campaign.setStatus(CampaignStatus.BEING_PROCESSED);
            campaignRepository.save(campaign);
            log.info("submiting the work to the async handler");
            creatorCampaignServiceAsyncEntry.endCampaignANdGenPdfEntry(campaign.getId(), OpenSlots);

        }


    }
}


