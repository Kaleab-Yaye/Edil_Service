package com.edil.repository;

import com.edil.domain.Account;
import com.edil.domain.Campaign;
import com.edil.domain.CampaignParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository

public interface CampaignParticipantsRepository extends JpaRepository<CampaignParticipant, UUID> {

    boolean existsByAccountIdAndCampaignId(UUID accountId, UUID campaignId);
    List<CampaignParticipant> findCampaignParticipantsByCampaign(Campaign campaign);
    boolean existsByEdilCode(String edilCode);

    Optional<CampaignParticipant> findCampaignParticipantsByEdilCodeAndCampaignId(String edilCode, UUID campaignId);


}
