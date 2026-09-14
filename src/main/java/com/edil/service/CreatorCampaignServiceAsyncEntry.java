package com.edil.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.UnknownUnits;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreatorCampaignServiceAsyncEntry



{ private  final  CreatorCampaignServiceTransactionalEntry creatorCampaignServiceTransactionalEntry;


    @Async(value = "endCampaignAndGeneratePdf")
    public void endCampaignANdGenPdfEntry(UUID campaignId, AtomicInteger atomicInteger){
        log.info("calling the transactional entry");
        creatorCampaignServiceTransactionalEntry.handleCampaignEndAndPdfGen(campaignId, atomicInteger);




    }
}

