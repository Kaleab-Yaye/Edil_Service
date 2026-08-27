package com.edil.repository;

import com.edil.domain.CreatorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreatorProfileRepository extends JpaRepository<CreatorProfile, UUID> {
    Optional<CreatorProfile> findByAccountId(UUID accountId);
    Optional<CreatorProfile> findByPhoneNumber(String phoneNumber);
}
