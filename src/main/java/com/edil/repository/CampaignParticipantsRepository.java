package com.edil.repository;

import com.edil.domain.Campaign;
import com.edil.domain.CampaignParticipants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository

public interface CampaignParticipantsRepository extends JpaRepository<CampaignParticipants, UUID> {

    boolean existsByAccountIdAndCampaignId(UUID accountId, UUID campaignId);
    List<CampaignParticipants> findCampaignParticipantsByCampaign(Campaign campaign);


}
