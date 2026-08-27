package com.edil.repository;

import com.edil.domain.ActiveCampaignPrize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActiveCampaignPrizeRepository extends JpaRepository<ActiveCampaignPrize, UUID> {
    List<ActiveCampaignPrize> findByCampaignIdOrderByPrizeOrderAsc(UUID campaignId);
    void deleteByCampaignId(UUID campaignId);
}
