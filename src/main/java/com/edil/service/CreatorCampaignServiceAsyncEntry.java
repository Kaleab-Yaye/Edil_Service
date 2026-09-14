package com.edil.service;


import org.checkerframework.checker.units.qual.UnknownUnits;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CreatorCampaignServiceAsyncEntry

{
    @Async(value = "endCampaignAndGeneratePdf")
    public void endCampaignANdGenPdfEntry(UUID campaignId, AtomicInteger atomicInteger){


    }
}

