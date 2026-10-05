package com.edil.service;

import com.edil.config.util.StoreCampaignToSlotHashMap;
import com.edil.domain.Account;
import com.edil.domain.ActiveCampaignPrize;
import com.edil.domain.ArchivedCampaignPrize;
import com.edil.domain.Campaign;
import com.edil.domain.CreatorProfile;
import com.edil.domain.enums.AccountRole;
import com.edil.domain.enums.CampaignStatus;
import com.edil.domain.enums.OnboardingStatus;
import com.edil.dto.request.CreateCampaignRequest;
import com.edil.dto.request.CreatePrizeRequest;
import com.edil.dto.response.CampaignDetailResponse;
import com.edil.dto.response.CampaignResponse;
import com.edil.dto.response.GetCampaignPaymentInfoResponse;
import com.edil.dto.response.GetNumberOfJoinedUsersResponse;
import com.edil.exception.AccountNotFoundException;
import com.edil.exception.CampaignNotFoundException;
import com.edil.repository.AccountRepository;
import com.edil.repository.ActiveCampaignPrizeRepository;
import com.edil.repository.ArchivedCampaignPrizeRepository;
import com.edil.repository.CampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private ActiveCampaignPrizeRepository activeCampaignPrizeRepository;

    @Mock
    private ArchivedCampaignPrizeRepository archivedCampaignPrizeRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UploadService uploadService;

    @Mock
    private CampaignServiceUtil campaignServiceUtil;

    @Mock
    private CampaignParticipantServiceUtil campaignParticipantServiceUtil;

    @Mock
    private StoreCampaignToSlotHashMap campaignToSlotHashMap;

    @InjectMocks
    private CampaignService campaignService;

    private Account creatorAccount;
    private CreatorProfile creatorProfile;
    private CreateCampaignRequest createCampaignRequest;
    private UUID creatorId;
    private UUID campaignId;

    @BeforeEach
    void setUp() {
        creatorId = UUID.randomUUID();
        campaignId = UUID.randomUUID();

        creatorAccount = Account.builder()
                .id(creatorId)
                .email("creator@edil.com")
                .role(AccountRole.CREATOR)
                .isActive(true)
                .build();

        creatorProfile = CreatorProfile.builder()
                .id(UUID.randomUUID())
                .account(creatorAccount)
                .fullName("Natnael Creator")
                .channelLink("https://t.me/natnael")
                .aboutSection("Channel description")
                .payoutBankAccount("100077889900")
                .onboardingStatus(OnboardingStatus.ONBOARDED)
                .build();

        creatorAccount.setCreatorProfile(creatorProfile);

        CreatePrizeRequest prize1 = CreatePrizeRequest.builder()
                .title("iPhone 15 Pro")
                .description("256GB Black Titanium")
                .prizeOrder(1)
                .imageFileId("img-file-123.jpg")
                .build();

        createCampaignRequest = CreateCampaignRequest.builder()
                .title("New Year Special Raffle")
                .aboutCampaign("Win latest gadgets")
                .ticketPrice(new BigDecimal("100.00"))
                .targetEntries(500)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .prizes(List.of(prize1))
                .build();
    }

    @Nested
    @DisplayName("createCampaign Tests")
    class CreateCampaignTests {

        @Test
        @DisplayName("Should successfully create campaign when all business rules are satisfied")
        void shouldCreateCampaignSuccessfully() {
            when(accountRepository.findByEmail(creatorAccount.getEmail())).thenReturn(Optional.of(creatorAccount));
            when(campaignRepository.existsByCreatorIdAndStatusIn(eq(creatorId), any())).thenReturn(false);
            when(uploadService.isUploadConfirmed("img-file-123.jpg")).thenReturn(true);

            Campaign savedCampaign = Campaign.builder()
                    .id(campaignId)
                    .creator(creatorAccount)
                    .title(createCampaignRequest.getTitle())
                    .aboutCampaign(createCampaignRequest.getAboutCampaign())
                    .ticketPrice(createCampaignRequest.getTicketPrice())
                    .targetEntries(createCampaignRequest.getTargetEntries())
                    .startDate(createCampaignRequest.getStartDate())
                    .endDate(createCampaignRequest.getEndDate())
                    .status(CampaignStatus.PENDING)
                    .activePrizes(List.of(ActiveCampaignPrize.builder()
                            .imageUrl("http://localhost:8081/static/img-file-123.jpg")
                            .build()))
                    .build();

            when(campaignRepository.save(any(Campaign.class))).thenReturn(savedCampaign);

            CampaignResponse response = campaignService.createCampaign(creatorAccount.getEmail(), createCampaignRequest);

            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo("New Year Special Raffle");
            assertThat(response.getCreatorName()).isEqualTo("Natnael Creator");
            assertThat(response.getFirstPrizeImageUrl()).isEqualTo("http://localhost:8081/static/img-file-123.jpg");
            assertThat(response.getStatus()).isEqualTo("PENDING");

            verify(uploadService).invalidateTicket("img-file-123.jpg");
            verify(campaignRepository).save(any(Campaign.class));
        }

        @Test
        @DisplayName("Should throw AccountNotFoundException if creator email does not exist")
        void shouldThrowWhenCreatorNotFound() {
            when(accountRepository.findByEmail("ghost@edil.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> campaignService.createCampaign("ghost@edil.com", createCampaignRequest))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessage("Creator account not found");
        }

        @Test
        @DisplayName("Should throw IllegalStateException if creator is not ONBOARDED")
        void shouldThrowWhenCreatorNotOnboarded() {
            creatorProfile.setOnboardingStatus(OnboardingStatus.PENDING);
            when(accountRepository.findByEmail(creatorAccount.getEmail())).thenReturn(Optional.of(creatorAccount));

            assertThatThrownBy(() -> campaignService.createCampaign(creatorAccount.getEmail(), createCampaignRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Creator must be ONBOARDED before creating campaigns");
        }

        @Test
        @DisplayName("Should throw IllegalStateException if creator profile is null")
        void shouldThrowWhenCreatorProfileIsNull() {
            creatorAccount.setCreatorProfile(null);
            when(accountRepository.findByEmail(creatorAccount.getEmail())).thenReturn(Optional.of(creatorAccount));

            assertThatThrownBy(() -> campaignService.createCampaign(creatorAccount.getEmail(), createCampaignRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Creator must be ONBOARDED before creating campaigns");
        }

        @Test
        @DisplayName("Should throw IllegalStateException if creator already has active or pending campaign")
        void shouldThrowWhenCreatorHasExistingActiveCampaign() {
            when(accountRepository.findByEmail(creatorAccount.getEmail())).thenReturn(Optional.of(creatorAccount));
            when(campaignRepository.existsByCreatorIdAndStatusIn(eq(creatorId), any())).thenReturn(true);

            assertThatThrownBy(() -> campaignService.createCampaign(creatorAccount.getEmail(), createCampaignRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Creator already has an active or pending campaign");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if endDate is before startDate")
        void shouldThrowWhenEndDateBeforeStartDate() {
            createCampaignRequest.setStartDate(LocalDateTime.now().plusDays(5));
            createCampaignRequest.setEndDate(LocalDateTime.now().plusDays(2)); // before start date

            when(accountRepository.findByEmail(creatorAccount.getEmail())).thenReturn(Optional.of(creatorAccount));
            when(campaignRepository.existsByCreatorIdAndStatusIn(eq(creatorId), any())).thenReturn(false);

            assertThatThrownBy(() -> campaignService.createCampaign(creatorAccount.getEmail(), createCampaignRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("End date must be after start date");
        }

        @Test
        @DisplayName("Should throw IllegalStateException if prize image upload is not confirmed")
        void shouldThrowWhenImageUploadNotConfirmed() {
            when(accountRepository.findByEmail(creatorAccount.getEmail())).thenReturn(Optional.of(creatorAccount));
            when(campaignRepository.existsByCreatorIdAndStatusIn(eq(creatorId), any())).thenReturn(false);
            when(uploadService.isUploadConfirmed("img-file-123.jpg")).thenReturn(false);

            assertThatThrownBy(() -> campaignService.createCampaign(creatorAccount.getEmail(), createCampaignRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Image upload not confirmed for file: img-file-123.jpg");

            verify(campaignRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Campaign Approval & Rejection Tests")
    class ApprovalRejectionTests {

        @Test
        @DisplayName("approveCampaign should set status to APPROVED and register in slot map")
        void approveCampaign_ShouldApproveAndRegisterSlots() {
            Campaign campaign = Campaign.builder().id(campaignId).status(CampaignStatus.PENDING).build();
            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));

            campaignService.approveCampaign(campaignId);

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.APPROVED);
            verify(campaignRepository).save(campaign);
            verify(campaignServiceUtil).addCampaignToCampaignToAvailableSlotMap(campaign);
        }

        @Test
        @DisplayName("rejectCampaign should set status to REJECTED")
        void rejectCampaign_ShouldReject() {
            Campaign campaign = Campaign.builder().id(campaignId).status(CampaignStatus.PENDING).build();
            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));

            campaignService.rejectCampaign(campaignId);

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.REJECTED);
            verify(campaignRepository).save(campaign);
        }

        @Test
        @DisplayName("approveCampaignEnd should approve end when campaign was ended by creator")
        void approveCampaignEnd_ShouldTransitionToEnded() {
            Campaign campaign = Campaign.builder().id(campaignId).status(CampaignStatus.ENDED_BY_CREATOR).build();
            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));

            campaignService.approveCampaignEnd(campaignId);

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ENDED);
            verify(campaignRepository).save(campaign);
        }

        @Test
        @DisplayName("approveCampaignEnd should throw IllegalStateException when status is not waiting for end approval")
        void approveCampaignEnd_ShouldThrowWhenStatusNotEnding() {
            Campaign campaign = Campaign.builder().id(campaignId).status(CampaignStatus.APPROVED).build();
            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));

            assertThatThrownBy(() -> campaignService.approveCampaignEnd(campaignId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Campaign is not waiting for end approval");
        }
    }

    @Nested
    @DisplayName("updateUserCount Tests")
    class UpdateUserCountTests {

        @Test
        @DisplayName("updateUserCount should increment count and return false when below target entries")
        void updateUserCount_ShouldIncrementAndReturnFalseWhenBelowTarget() {
            Campaign campaign = Campaign.builder()
                    .id(campaignId)
                    .targetEntries(100)
                    .joinedUsers(50)
                    .status(CampaignStatus.APPROVED)
                    .build();

            when(campaignRepository.getCampaignsById(campaignId)).thenReturn(Optional.of(campaign));

            boolean targetReached = campaignService.updateUserCount(campaignId);

            assertThat(targetReached).isFalse();
            assertThat(campaign.getJoinedUsers()).isEqualTo(51);
            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.APPROVED);
            verify(campaignRepository).save(campaign);
            verify(activeCampaignPrizeRepository, never()).deleteByCampaignId(any());
        }

        @Test
        @DisplayName("updateUserCount should set status to ENDED and archive prizes when target reached")
        void updateUserCount_ShouldEndCampaignAndArchiveWhenTargetReached() {
            Campaign campaign = Campaign.builder()
                    .id(campaignId)
                    .targetEntries(100)
                    .joinedUsers(99)
                    .status(CampaignStatus.APPROVED)
                    .build();

            when(campaignRepository.getCampaignsById(campaignId)).thenReturn(Optional.of(campaign));
            when(activeCampaignPrizeRepository.findByCampaignIdOrderByPrizeOrderAsc(campaignId)).thenReturn(List.of(
                    ActiveCampaignPrize.builder().id(UUID.randomUUID()).title("Prize 1").prizeOrder(1).imageUrl("url1").build()
            ));

            boolean targetReached = campaignService.updateUserCount(campaignId);

            assertThat(targetReached).isTrue();
            assertThat(campaign.getJoinedUsers()).isEqualTo(100);
            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ENDED);
            assertThat(campaign.getTargetReachedAt()).isNotNull();

            verify(campaignRepository).save(campaign);
            verify(archivedCampaignPrizeRepository).saveAll(anyList());
            verify(activeCampaignPrizeRepository).deleteByCampaignId(campaignId);
        }
    }

    @Nested
    @DisplayName("getCampaignPaymentInfo Tests")
    class PaymentInfoTests {

        @Test
        @DisplayName("getCampaignPaymentInfo should return 200 OK with payout bank account and ticket price for APPROVED campaign")
        void getCampaignPaymentInfo_ShouldReturnOkWhenApproved() {
            Campaign campaign = Campaign.builder()
                    .id(campaignId)
                    .status(CampaignStatus.APPROVED)
                    .ticketPrice(new BigDecimal("50.00"))
                    .creator(creatorAccount)
                    .build();

            when(campaignRepository.getCampaignsById(campaignId)).thenReturn(Optional.of(campaign));

            ResponseEntity<GetCampaignPaymentInfoResponse> response = campaignService.getCampaignPaymentInfo(campaignId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().ticketPrice()).isEqualTo(new BigDecimal("50.00"));
            assertThat(response.getBody().accountNumber()).isEqualTo("100077889900");
            assertThat(response.getBody().accountHolderName()).isEqualTo("Natnael Creator");
        }

        @Test
        @DisplayName("getCampaignPaymentInfo should return 406 NOT_ACCEPTABLE for non-approved campaign")
        void getCampaignPaymentInfo_ShouldReturnNotAcceptableWhenNotApproved() {
            Campaign campaign = Campaign.builder()
                    .id(campaignId)
                    .status(CampaignStatus.PENDING)
                    .build();

            when(campaignRepository.getCampaignsById(campaignId)).thenReturn(Optional.of(campaign));

            ResponseEntity<GetCampaignPaymentInfoResponse> response = campaignService.getCampaignPaymentInfo(campaignId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @Nested
    @DisplayName("getNumberOfJoinedUsers Tests")
    class JoinedUsersCountTests {

        @Test
        @DisplayName("getNumberOfJoinedUsers should return 200 OK when requested by creator")
        void getNumberOfJoinedUsers_ShouldAllowCreator() {
            Campaign campaign = Campaign.builder()
                    .id(campaignId)
                    .joinedUsers(42)
                    .creator(creatorAccount)
                    .build();

            when(campaignRepository.getCampaignsById(campaignId)).thenReturn(Optional.of(campaign));
            when(accountRepository.findByEmail(creatorAccount.getEmail())).thenReturn(Optional.of(creatorAccount));

            ResponseEntity<GetNumberOfJoinedUsersResponse> response =
                    campaignService.getNumberOfJoinedUsers(campaignId, creatorAccount.getEmail());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().joinedUsers()).isEqualTo(42);
        }

        @Test
        @DisplayName("getNumberOfJoinedUsers should return 200 OK when requested by ADMIN")
        void getNumberOfJoinedUsers_ShouldAllowAdmin() {
            Campaign campaign = Campaign.builder()
                    .id(campaignId)
                    .joinedUsers(42)
                    .creator(creatorAccount)
                    .build();

            Account admin = Account.builder()
                    .id(UUID.randomUUID())
                    .email("admin@edil.com")
                    .role(AccountRole.ADMIN)
                    .build();

            when(campaignRepository.getCampaignsById(campaignId)).thenReturn(Optional.of(campaign));
            when(accountRepository.findByEmail("admin@edil.com")).thenReturn(Optional.of(admin));

            ResponseEntity<GetNumberOfJoinedUsersResponse> response =
                    campaignService.getNumberOfJoinedUsers(campaignId, "admin@edil.com");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().joinedUsers()).isEqualTo(42);
        }

        @Test
        @DisplayName("getNumberOfJoinedUsers should return 403 FORBIDDEN when requested by random user")
        void getNumberOfJoinedUsers_ShouldRejectUnauthorizedUser() {
            Campaign campaign = Campaign.builder()
                    .id(campaignId)
                    .joinedUsers(42)
                    .creator(creatorAccount)
                    .build();

            Account otherUser = Account.builder()
                    .id(UUID.randomUUID())
                    .email("other@edil.com")
                    .role(AccountRole.USER)
                    .build();

            when(campaignRepository.getCampaignsById(campaignId)).thenReturn(Optional.of(campaign));
            when(accountRepository.findByEmail("other@edil.com")).thenReturn(Optional.of(otherUser));

            ResponseEntity<GetNumberOfJoinedUsersResponse> response =
                    campaignService.getNumberOfJoinedUsers(campaignId, "other@edil.com");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}
