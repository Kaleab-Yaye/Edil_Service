package com.edil.repository;


import com.edil.domain.ArchivedCampaignParticipants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArchivedCampaignParticipantsRepository extends JpaRepository<com.edil.domain.ArchivedCampaignParticipants, UUID> {
    Optional<ArchivedCampaignParticipants> findArchivedCampaignParticipantsByEdilCodeAndCampaignId(String edilCode, UUID campaignId);
}
