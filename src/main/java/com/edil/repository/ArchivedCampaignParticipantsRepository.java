package com.edil.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ArchivedCampaignParticipantsRepository extends JpaRepository<com.edil.domain.ArchivedCampaignParticipants, UUID> {
}
