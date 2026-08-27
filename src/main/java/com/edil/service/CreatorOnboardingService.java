package com.edil.service;

import com.edil.domain.Account;
import com.edil.domain.CreatorOnboardingQueue;
import com.edil.domain.CreatorProfile;
import com.edil.domain.enums.OnboardingStatus;
import com.edil.dto.response.CreatorQueueItemResponse;
import com.edil.exception.AccountNotFoundException;
import com.edil.repository.AccountRepository;
import com.edil.repository.CreatorOnboardingQueueRepository;
import com.edil.repository.CreatorProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreatorOnboardingService {

    private final CreatorOnboardingQueueRepository queueRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<CreatorQueueItemResponse> getQueue() {
        List<CreatorOnboardingQueue> queue = queueRepository.findAllByOrderBySubmittedAtAsc();
        return queue.stream().map(entry -> {
            Account account = entry.getAccount();
            CreatorProfile profile = creatorProfileRepository.findByAccountId(account.getId())
                    .orElseThrow(() -> new AccountNotFoundException("Creator profile not found"));
            return CreatorQueueItemResponse.builder()
                    .accountId(account.getId())
                    .email(account.getEmail())
                    .fullName(profile.getFullName())
                    .phoneNumber(profile.getPhoneNumber())
                    .channelLink(profile.getChannelLink())
                    .aboutSection(profile.getAboutSection())
                    .payoutBankAccount(profile.getPayoutBankAccount())
                    .onboardingStatus(profile.getOnboardingStatus().name())
                    .submittedAt(entry.getSubmittedAt())
                    .build();
        }).toList();
    }

    @Transactional
    public void approveCreator(UUID accountId) {
        CreatorProfile profile = creatorProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Creator profile not found"));
        profile.setOnboardingStatus(OnboardingStatus.ONBOARDED);
        creatorProfileRepository.save(profile);
        queueRepository.deleteByAccountId(accountId);
    }

    @Transactional
    public void rejectCreator(UUID accountId) {
        CreatorProfile profile = creatorProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Creator profile not found"));
        profile.setOnboardingStatus(OnboardingStatus.DECLINED);
        creatorProfileRepository.save(profile);
        queueRepository.deleteByAccountId(accountId);
    }

    @Transactional
    public void reapply(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        CreatorProfile profile = creatorProfileRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new AccountNotFoundException("Creator profile not found"));
        if (profile.getOnboardingStatus() != OnboardingStatus.DECLINED) {
            throw new IllegalStateException("Can only re-apply from DECLINED status");
        }
        profile.setOnboardingStatus(OnboardingStatus.PENDING);
        creatorProfileRepository.save(profile);

        // Re-insert into queue if not already there
        if (queueRepository.findByAccountId(account.getId()).isEmpty()) {
            CreatorOnboardingQueue entry = CreatorOnboardingQueue.builder()
                    .account(account)
                    .build();
            queueRepository.save(entry);
        }
    }
}
