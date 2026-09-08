package com.edil.repository;

import com.edil.domain.Petition;
import com.edil.domain.enums.PetitionStatus;
import org.checkerframework.common.util.count.report.qual.ReportCreation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface PetitionRepository extends JpaRepository<Petition, UUID> {
    boolean existsByPetitionerIdAndCampaignIdAndStatus(UUID userId, UUID campaignId,PetitionStatus petitionStatus);
    Page<Petition> getPetitionsByStatus(PetitionStatus petitionStatus, Pageable pageable);
    Optional<Petition> getPetitionsById(UUID petitionId);
}
