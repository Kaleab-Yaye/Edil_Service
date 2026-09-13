package com.edil.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class CampaignScheduler {

    private final CampaignService campaignService;

    @Scheduled(cron = "0 * * * * *")
    public void archiveExpiredCampaigns() {
        campaignService.archiveExpiredCampaigns();
    }
}


