package com.edil.repository;

import com.edil.domain.CreatorOnboardingQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreatorOnboardingQueueRepository extends JpaRepository<CreatorOnboardingQueue, UUID> {
    Optional<CreatorOnboardingQueue> findByAccountId(UUID accountId);
    List<CreatorOnboardingQueue> findAllByOrderBySubmittedAtAsc();
    void deleteByAccountId(UUID accountId);
}
