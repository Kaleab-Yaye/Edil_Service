package com.edil.repository;


import com.edil.domain.Campaign;
import com.edil.domain.CampaignParticipantsPdf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignParticipantPdfRepository extends JpaRepository<CampaignParticipantsPdf, UUID> {

    Optional<CampaignParticipantsPdf> getCampaignParticipantsPdfByCampaign(Campaign campaign);
    Optional<CampaignParticipantsPdf> getCampaignParticipantsPdfByCampaignId(UUID campaignId);

}
