package com.edil.service;


import com.edil.controller.CampaignParticipantController;
import com.edil.domain.Campaign;
import com.edil.domain.enums.CampaignStatus;
import com.edil.exception.CampaignNotFoundException;
import com.edil.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreatorCampaignServiceTransactionalEntry {

    private final CampaignRepository campaignRepository;
    private final ArchivedCampaignParticipantsRepository archivedCampaignParticipantsRepository;
    private final ArchivedCampaignPrizeRepository archivedCampaignPrizeRepository;
    private final CampaignParticipantPdfRepository campaignParticipantPdfRepository;
    private  final CampaignParticipantsRepository campaignParticipantsRepository;


    @Transactional
    public void handleCampaignEndAndPdfGen(UUID campaignID){
        Campaign campaign = campaignRepository.getCampaignsById(campaignID).orElseThrow(()-> new CampaignNotFoundException(campaignID.toString()));
        if (!campaign.getStatus().equals(CampaignStatus.APPROVED)){
            log.info("another async methode handled the job for the campaign with the ID:{} already", campaign.getId());
            return;
        }

        campaign.setStatus(CampaignStatus.BEING_PROCESSED);
        campaignRepository.save(campaign);



        // will now call the combined util for handling pdf generation and moving of the end particiapnts in to archive



    }


}
