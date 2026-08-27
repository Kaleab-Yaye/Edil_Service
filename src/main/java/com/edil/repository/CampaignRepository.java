package com.edil.repository;

import com.edil.domain.Campaign;
import com.edil.domain.enums.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, UUID> {
    Page<Campaign> findByStatus(CampaignStatus status, Pageable pageable);
    List<Campaign> findByStatusAndEndDateLessThanEqual(CampaignStatus status, LocalDateTime now);
    List<Campaign> findByCreatorId(UUID creatorId);
}
