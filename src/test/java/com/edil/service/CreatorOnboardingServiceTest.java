package com.edil.service;

import com.edil.domain.Account;
import com.edil.domain.CreatorOnboardingQueue;
import com.edil.domain.CreatorProfile;
import com.edil.domain.enums.AccountRole;
import com.edil.domain.enums.OnboardingStatus;
import com.edil.dto.response.CreatorQueueItemResponse;
import com.edil.exception.AccountNotFoundException;
import com.edil.repository.AccountRepository;
import com.edil.repository.CreatorOnboardingQueueRepository;
import com.edil.repository.CreatorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatorOnboardingServiceTest {

    @Mock
    private CreatorOnboardingQueueRepository queueRepository;

    @Mock
    private CreatorProfileRepository creatorProfileRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private CreatorOnboardingService onboardingService;

    private UUID accountId;
    private Account account;
    private CreatorProfile creatorProfile;
    private CreatorOnboardingQueue queueEntry;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();

        account = Account.builder()
                .id(accountId)
                .email("creator@edil.com")
                .role(AccountRole.CREATOR)
                .isActive(true)
                .build();

        creatorProfile = CreatorProfile.builder()
                .id(UUID.randomUUID())
                .account(account)
                .fullName("Marta Goitom")
                .phoneNumber("+251911001122")
                .channelLink("https://t.me/marta_channel")
                .aboutSection("Content creator from Addis")
                .payoutBankAccount("100055554444")
                .onboardingStatus(OnboardingStatus.PENDING)
                .build();

        queueEntry = CreatorOnboardingQueue.builder()
                .id(UUID.randomUUID())
                .account(account)
                .submittedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("getQueue should return empty list when no entries exist")
    void getQueue_ShouldReturnEmptyListWhenQueueIsEmpty() {
        when(queueRepository.findAllByOrderBySubmittedAtAsc()).thenReturn(Collections.emptyList());

        List<CreatorQueueItemResponse> result = onboardingService.getQueue();

        assertThat(result).isEmpty();
        verify(queueRepository).findAllByOrderBySubmittedAtAsc();
    }

    @Test
    @DisplayName("getQueue should map queue entries and profiles correctly")
    void getQueue_ShouldReturnMappedResponses() {
        when(queueRepository.findAllByOrderBySubmittedAtAsc()).thenReturn(List.of(queueEntry));
        when(creatorProfileRepository.findByAccountId(accountId)).thenReturn(Optional.of(creatorProfile));

        List<CreatorQueueItemResponse> result = onboardingService.getQueue();

        assertThat(result).hasSize(1);
        CreatorQueueItemResponse item = result.get(0);
        assertThat(item.getAccountId()).isEqualTo(accountId);
        assertThat(item.getEmail()).isEqualTo("creator@edil.com");
        assertThat(item.getFullName()).isEqualTo("Marta Goitom");
        assertThat(item.getPhoneNumber()).isEqualTo("+251911001122");
        assertThat(item.getChannelLink()).isEqualTo("https://t.me/marta_channel");
        assertThat(item.getPayoutBankAccount()).isEqualTo("100055554444");
        assertThat(item.getOnboardingStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("getQueue should throw AccountNotFoundException if profile is missing for queue entry")
    void getQueue_ShouldThrowWhenProfileNotFound() {
        when(queueRepository.findAllByOrderBySubmittedAtAsc()).thenReturn(List.of(queueEntry));
        when(creatorProfileRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> onboardingService.getQueue())
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Creator profile not found");
    }

    @Test
    @DisplayName("approveCreator should set status to ONBOARDED and remove entry from queue")
    void approveCreator_ShouldUpdateStatusAndRemoveFromQueue() {
        when(creatorProfileRepository.findByAccountId(accountId)).thenReturn(Optional.of(creatorProfile));

        onboardingService.approveCreator(accountId);

        ArgumentCaptor<CreatorProfile> captor = ArgumentCaptor.forClass(CreatorProfile.class);
        verify(creatorProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getOnboardingStatus()).isEqualTo(OnboardingStatus.ONBOARDED);

        verify(queueRepository).deleteByAccountId(accountId);
    }

    @Test
    @DisplayName("approveCreator should throw AccountNotFoundException if profile does not exist")
    void approveCreator_ShouldThrowWhenProfileNotFound() {
        when(creatorProfileRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> onboardingService.approveCreator(accountId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Creator profile not found");

        verify(creatorProfileRepository, never()).save(any());
        verify(queueRepository, never()).deleteByAccountId(any());
    }

    @Test
    @DisplayName("rejectCreator should set status to DECLINED and remove entry from queue")
    void rejectCreator_ShouldUpdateStatusToDeclinedAndRemoveFromQueue() {
        when(creatorProfileRepository.findByAccountId(accountId)).thenReturn(Optional.of(creatorProfile));

        onboardingService.rejectCreator(accountId);

        ArgumentCaptor<CreatorProfile> captor = ArgumentCaptor.forClass(CreatorProfile.class);
        verify(creatorProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getOnboardingStatus()).isEqualTo(OnboardingStatus.DECLINED);

        verify(queueRepository).deleteByAccountId(accountId);
    }

    @Test
    @DisplayName("rejectCreator should throw AccountNotFoundException if profile does not exist")
    void rejectCreator_ShouldThrowWhenProfileNotFound() {
        when(creatorProfileRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> onboardingService.rejectCreator(accountId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Creator profile not found");

        verify(creatorProfileRepository, never()).save(any());
        verify(queueRepository, never()).deleteByAccountId(any());
    }

    @Test
    @DisplayName("reapply should transition DECLINED creator back to PENDING and re-enqueue")
    void reapply_ShouldSucceedForDeclinedCreator() {
        creatorProfile.setOnboardingStatus(OnboardingStatus.DECLINED);

        when(accountRepository.findByEmail(account.getEmail())).thenReturn(Optional.of(account));
        when(creatorProfileRepository.findByAccountId(accountId)).thenReturn(Optional.of(creatorProfile));
        when(queueRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

        onboardingService.reapply(account.getEmail());

        ArgumentCaptor<CreatorProfile> profileCaptor = ArgumentCaptor.forClass(CreatorProfile.class);
        verify(creatorProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getOnboardingStatus()).isEqualTo(OnboardingStatus.PENDING);

        ArgumentCaptor<CreatorOnboardingQueue> queueCaptor = ArgumentCaptor.forClass(CreatorOnboardingQueue.class);
        verify(queueRepository).save(queueCaptor.capture());
        assertThat(queueCaptor.getValue().getAccount()).isEqualTo(account);
    }

    @Test
    @DisplayName("reapply should not duplicate queue entry if already in queue")
    void reapply_ShouldNotDuplicateQueueIfAlreadyPresent() {
        creatorProfile.setOnboardingStatus(OnboardingStatus.DECLINED);

        when(accountRepository.findByEmail(account.getEmail())).thenReturn(Optional.of(account));
        when(creatorProfileRepository.findByAccountId(accountId)).thenReturn(Optional.of(creatorProfile));
        when(queueRepository.findByAccountId(accountId)).thenReturn(Optional.of(queueEntry));

        onboardingService.reapply(account.getEmail());

        verify(creatorProfileRepository).save(any(CreatorProfile.class));
        verify(queueRepository, never()).save(any());
    }

    @Test
    @DisplayName("reapply should throw IllegalStateException if creator is not in DECLINED status")
    void reapply_ShouldThrowWhenNotDeclined() {
        creatorProfile.setOnboardingStatus(OnboardingStatus.ONBOARDED);

        when(accountRepository.findByEmail(account.getEmail())).thenReturn(Optional.of(account));
        when(creatorProfileRepository.findByAccountId(accountId)).thenReturn(Optional.of(creatorProfile));

        assertThatThrownBy(() -> onboardingService.reapply(account.getEmail()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Can only re-apply from DECLINED status");

        verify(creatorProfileRepository, never()).save(any());
        verify(queueRepository, never()).save(any());
    }

    @Test
    @DisplayName("reapply should throw AccountNotFoundException when account does not exist")
    void reapply_ShouldThrowWhenAccountNotFound() {
        when(accountRepository.findByEmail("unknown@edil.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> onboardingService.reapply("unknown@edil.com"))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account not found");
    }
}
