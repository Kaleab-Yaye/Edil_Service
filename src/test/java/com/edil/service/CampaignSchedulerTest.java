package com.edil.service;

import com.edil.domain.Campaign;
import com.edil.domain.enums.CampaignStatus;
import com.edil.repository.CampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignSchedulerTest {

    @Mock
    private CampaignService campaignService;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CreatorCampaignServiceAsyncEntry creatorCampaignServiceAsyncEntry;

    @InjectMocks
    private CampaignScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler.OpenSlots.set(100);
    }

    @Test
    @DisplayName("Should return immediately when OpenSlots is 0")
    void shouldReturnEarlyWhenNoSlotsAvailable() {
        scheduler.OpenSlots.set(0);

        scheduler.archiveExpiredCampaignsAndGeneratePdf();

        verify(campaignService, never()).getPageableByStatusAndTime(any(), any(), any());
        verify(campaignService, never()).getPageableByStatus(any(), any());
        verify(creatorCampaignServiceAsyncEntry, never()).endCampaignANdGenPdfEntry(any(), any());
    }

    @Test
    @DisplayName("Should process stuck campaigns and ended campaigns, dispatching to async PDF entry")
    void shouldProcessAndDispatchCampaigns() {
        UUID campaignId1 = UUID.randomUUID();
        Campaign stuckCampaign = Campaign.builder()
                .id(campaignId1)
                .status(CampaignStatus.BEING_PROCESSED)
                .build();

        UUID campaignId2 = UUID.randomUUID();
        Campaign endedCampaign = Campaign.builder()
                .id(campaignId2)
                .status(CampaignStatus.ENDED_BY_CREATOR)
                .build();

        when(campaignService.getPageableByStatusAndTime(eq(CampaignStatus.BEING_PROCESSED), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(stuckCampaign));

        when(campaignService.getPageableByStatus(eq(CampaignStatus.ENDED_BY_CREATOR), any(Pageable.class)))
                .thenReturn(List.of(endedCampaign));

        scheduler.archiveExpiredCampaignsAndGeneratePdf();

        // Verify both campaigns updated to BEING_PROCESSED and saved
        verify(campaignRepository).save(stuckCampaign);
        verify(campaignRepository).save(endedCampaign);
        assertThat(stuckCampaign.getStatus()).isEqualTo(CampaignStatus.BEING_PROCESSED);
        assertThat(endedCampaign.getStatus()).isEqualTo(CampaignStatus.BEING_PROCESSED);

        // Verify async entry was invoked for each campaign
        verify(creatorCampaignServiceAsyncEntry).endCampaignANdGenPdfEntry(eq(campaignId1), eq(scheduler.OpenSlots));
        verify(creatorCampaignServiceAsyncEntry).endCampaignANdGenPdfEntry(eq(campaignId2), eq(scheduler.OpenSlots));
    }

    @Test
    @DisplayName("Should handle empty results gracefully without errors")
    void shouldHandleEmptyResults() {
        when(campaignService.getPageableByStatusAndTime(eq(CampaignStatus.BEING_PROCESSED), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        when(campaignService.getPageableByStatus(eq(CampaignStatus.ENDED_BY_CREATOR), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        scheduler.archiveExpiredCampaignsAndGeneratePdf();

        verify(campaignRepository, never()).save(any());
        verify(creatorCampaignServiceAsyncEntry, never()).endCampaignANdGenPdfEntry(any(), any());
        assertThat(scheduler.OpenSlots.get()).isEqualTo(100);
    }
}
