package com.edil.service;


import com.edil.controller.CampaignParticipantController;
import com.edil.domain.Campaign;
import com.edil.domain.enums.CampaignStatus;
import com.edil.exception.CampaignNotFoundException;
import com.edil.repository.*;
import com.edil.util.ArchiveCampaignAndGeneratePdfUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreatorCampaignServiceTransactionalEntry {

    private final CampaignRepository campaignRepository;
    private final ArchivedCampaignParticipantsRepository archivedCampaignParticipantsRepository;
    private final ArchivedCampaignPrizeRepository archivedCampaignPrizeRepository;
    private final CampaignParticipantPdfRepository campaignParticipantPdfRepository;
    private  final CampaignParticipantsRepository campaignParticipantsRepository;
    private  final ArchiveCampaignAndGeneratePdfUtil archiveCampaignAndGeneratePdfUtil;


    @Transactional
    public void handleCampaignEndAndPdfGen(UUID campaignID, AtomicInteger atomicInteger){
        archiveCampaignAndGeneratePdfUtil.handleArchivalAndGeneratePdfUtil(archivedCampaignParticipantsRepository,
                archivedCampaignPrizeRepository,
                campaignParticipantPdfRepository,
                campaignRepository,
                campaignParticipantsRepository,
                campaignID,
                atomicInteger

                );
        // will now call the combined util for handling pdf generation and moving of the end particiapnts in to archive



    }


}
