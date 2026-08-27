package com.edil.repository;

import com.edil.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByAccountId(UUID accountId);
    Optional<UserProfile> findByPhoneNumber(String phoneNumber);
}
