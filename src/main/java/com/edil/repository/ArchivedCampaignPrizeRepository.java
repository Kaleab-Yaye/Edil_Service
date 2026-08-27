package com.edil.repository;

import com.edil.domain.ArchivedCampaignPrize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ArchivedCampaignPrizeRepository extends JpaRepository<ArchivedCampaignPrize, UUID> {
    List<ArchivedCampaignPrize> findByCampaignIdOrderByPrizeOrderAsc(UUID campaignId);
}
