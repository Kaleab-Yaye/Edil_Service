package com.edil.service;

import com.edil.config.util.StoreCampaignToSlotHashMap;
import com.edil.domain.Account;
import com.edil.domain.Campaign;
import com.edil.domain.CreatorProfile;
import com.edil.domain.Slot;
import com.edil.domain.UserProfile;
import com.edil.domain.enums.CampaignStatus;
import com.edil.dto.internal.SlotKeyToCampaignAndUserIdDto;
import com.edil.dto.request.CanParticipantJoinCampaignRequest;
import com.edil.dto.request.CreateCampaignSlotForUserRequest;
import com.edil.dto.response.CanParticipantJoinCampaignResponse;
import com.edil.dto.response.CreateCampaignSlotForUserResponse;
import com.edil.dto.request.FetchOnGoingSlotInformationForUserResponse;
import com.edil.repository.ArchivedCampaignParticipantsRepository;
import com.edil.repository.CampaignParticipantsRepository;
import com.edil.repository.ReceiptRepository;
import com.edil.repository.SlotRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignParticipantServiceTest {

    @Mock
    private CampaignParticipantServiceUtil campaignParticipantServiceUtil;

    @Mock
    private UserService userService;

    @Mock
    private CampaignParticipantsRepository campaignParticipantsRepository;

    @Mock
    private CampaignService campaignService;

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private ReceiptService receiptService;

    @Mock
    private AuthService authService;

    @Mock
    private ArchivedCampaignParticipantsRepository archivedCampaignParticipantsRepository;

    @Mock
    private PetitionService petitionService;

    @Mock
    private StoreCampaignToSlotHashMap storeCampaignToSlotHashMap;

    private Cache<UUID, SlotKeyToCampaignAndUserIdDto> slotKeyToCampaignIdCache;
    private Cache<String, UUID> userEmailToSlotAvailableCheckCache;
    private Cache<UUID, Boolean> receiptUploadKeyCache;

    private CampaignParticipantService participantService;

    private UUID campaignId;
    private UUID userId;
    private String userEmail;
    private Account userAccount;
    private UserProfile userProfile;
    private Campaign approvedCampaign;

    @BeforeEach
    void setUp() {
        slotKeyToCampaignIdCache = Caffeine.newBuilder().build();
        userEmailToSlotAvailableCheckCache = Caffeine.newBuilder().build();
        receiptUploadKeyCache = Caffeine.newBuilder().build();

        participantService = new CampaignParticipantService(
                campaignParticipantServiceUtil,
                userService,
                campaignParticipantsRepository,
                campaignService,
                receiptRepository,
                slotRepository,
                slotKeyToCampaignIdCache,
                userEmailToSlotAvailableCheckCache,
                receiptService,
                authService,
                receiptUploadKeyCache,
                archivedCampaignParticipantsRepository,
                petitionService,
                storeCampaignToSlotHashMap
        );

        campaignId = UUID.randomUUID();
        userId = UUID.randomUUID();
        userEmail = "tester@edil.com";

        userAccount = Account.builder().id(userId).email(userEmail).isActive(true).build();
        userProfile = UserProfile.builder().id(UUID.randomUUID()).account(userAccount).build();
        userAccount.setUserProfile(userProfile);

        CreatorProfile creatorProfile = CreatorProfile.builder()
                .fullName("Host Creator")
                .payoutBankAccount("1000998877")
                .build();
        Account creator = Account.builder().creatorProfile(creatorProfile).build();

        approvedCampaign = Campaign.builder()
                .id(campaignId)
                .status(CampaignStatus.APPROVED)
                .ticketPrice(new BigDecimal("25.00"))
                .creator(creator)
                .build();

        // Initialize static store for this campaign
        StoreCampaignToSlotHashMap.campaignToSlotStore.put(campaignId, new AtomicInteger(10));
    }

    @AfterEach
    void tearDown() {
        StoreCampaignToSlotHashMap.campaignToSlotStore.clear();
    }

    @Nested
    @DisplayName("canParticipantJoinCampaign Tests")
    class CanParticipantJoinTests {

        @Test
        @DisplayName("Should return 200 OK when user is eligible to join")
        void shouldAllowEligibleUser() {
            when(userService.getAccountIDByEmail(userEmail)).thenReturn(userId);
            when(userService.getAccountByEmail(userEmail)).thenReturn(userAccount);
            when(campaignParticipantsRepository.existsByAccountIdAndCampaignId(userId, campaignId)).thenReturn(false);
            when(petitionService.doesUserHasOngoingPetitionForCampaign(campaignId, userProfile.getId())).thenReturn(false);

            ResponseEntity<CanParticipantJoinCampaignResponse> response =
                    participantService.canParticipantJoinCampaign(new CanParticipantJoinCampaignRequest(campaignId), userEmail);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().canJoin()).isTrue();
            assertThat(response.getBody().hasReservedSlot()).isFalse();
            assertThat(response.getBody().haOnGoingPetition()).isFalse();
        }

        @Test
        @DisplayName("Should return 409 CONFLICT if user already joined this campaign")
        void shouldRejectAlreadyJoinedUser() {
            when(userService.getAccountIDByEmail(userEmail)).thenReturn(userId);
            when(userService.getAccountByEmail(userEmail)).thenReturn(userAccount);
            when(campaignParticipantsRepository.existsByAccountIdAndCampaignId(userId, campaignId)).thenReturn(true);

            ResponseEntity<CanParticipantJoinCampaignResponse> response =
                    participantService.canParticipantJoinCampaign(new CanParticipantJoinCampaignRequest(campaignId), userEmail);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().canJoin()).isFalse();
        }

        @Test
        @DisplayName("Should return 409 CONFLICT if user already has a reserved slot in cache")
        void shouldRejectUserWithReservedSlot() {
            UUID slotKey = UUID.randomUUID();
            userEmailToSlotAvailableCheckCache.put(userEmail, slotKey);
            slotKeyToCampaignIdCache.put(slotKey, SlotKeyToCampaignAndUserIdDto.returnSlotKeyToCampaignAndUserIdDtoWithTime(campaignId, userEmail));

            when(userService.getAccountIDByEmail(userEmail)).thenReturn(userId);
            when(userService.getAccountByEmail(userEmail)).thenReturn(userAccount);
            when(campaignParticipantsRepository.existsByAccountIdAndCampaignId(userId, campaignId)).thenReturn(false);

            ResponseEntity<CanParticipantJoinCampaignResponse> response =
                    participantService.canParticipantJoinCampaign(new CanParticipantJoinCampaignRequest(campaignId), userEmail);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().hasReservedSlot()).isTrue();
            assertThat(response.getBody().campaignId()).isEqualTo(campaignId);
            assertThat(response.getBody().slotKey()).isEqualTo(slotKey);
        }

        @Test
        @DisplayName("Should return 409 CONFLICT if user has an ongoing unresolved petition")
        void shouldRejectUserWithOngoingPetition() {
            when(userService.getAccountIDByEmail(userEmail)).thenReturn(userId);
            when(userService.getAccountByEmail(userEmail)).thenReturn(userAccount);
            when(campaignParticipantsRepository.existsByAccountIdAndCampaignId(userId, campaignId)).thenReturn(false);
            when(petitionService.doesUserHasOngoingPetitionForCampaign(campaignId, userProfile.getId())).thenReturn(true);

            ResponseEntity<CanParticipantJoinCampaignResponse> response =
                    participantService.canParticipantJoinCampaign(new CanParticipantJoinCampaignRequest(campaignId), userEmail);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().haOnGoingPetition()).isTrue();
        }
    }

    @Nested
    @DisplayName("createCampaignSlotForUser Tests")
    class CreateCampaignSlotTests {

        @Test
        @DisplayName("Should successfully allocate slot, decrement slot store, and populate caches")
        void shouldAllocateSlotSuccessfully() {
            when(campaignService.getCampaignById(campaignId)).thenReturn(Optional.of(approvedCampaign));
            when(userService.getAccountIDByEmail(userEmail)).thenReturn(userId);
            when(campaignParticipantsRepository.existsByAccountIdAndCampaignId(userId, campaignId)).thenReturn(false);

            when(slotRepository.save(any(Slot.class))).thenAnswer(invocation -> {
                Slot s = invocation.getArgument(0);
                ReflectionTestUtils.setField(s, "id", UUID.randomUUID());
                return s;
            });

            ResponseEntity<CreateCampaignSlotForUserResponse> response =
                    participantService.createCampaignSlotForUser(new CreateCampaignSlotForUserRequest(campaignId), userEmail);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().slotAvailable()).isTrue();
            assertThat(response.getBody().amountTOPay()).isEqualTo(new BigDecimal("25.00"));
            assertThat(response.getBody().nameOfAccountHolder()).isEqualTo("Host Creator");
            assertThat(response.getBody().accountNumber()).isEqualTo("1000998877");

            // Verify cache entries populated
            assertThat(userEmailToSlotAvailableCheckCache.getIfPresent(userEmail)).isNotNull();
            // Verify slot count decremented from 10 to 9
            assertThat(StoreCampaignToSlotHashMap.campaignToSlotStore.get(campaignId).get()).isEqualTo(9);
        }

        @Test
        @DisplayName("Should return 406 NOT_ACCEPTABLE if campaign is not APPROVED")
        void shouldRejectWhenCampaignNotApproved() {
            approvedCampaign.setStatus(CampaignStatus.PENDING);
            when(campaignService.getCampaignById(campaignId)).thenReturn(Optional.of(approvedCampaign));

            ResponseEntity<CreateCampaignSlotForUserResponse> response =
                    participantService.createCampaignSlotForUser(new CreateCampaignSlotForUserRequest(campaignId), userEmail);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
        }

        @Test
        @DisplayName("Should return 409 CONFLICT if user already has a slot reserved")
        void shouldRejectWhenUserAlreadyHasSlot() {
            userEmailToSlotAvailableCheckCache.put(userEmail, UUID.randomUUID());

            when(campaignService.getCampaignById(campaignId)).thenReturn(Optional.of(approvedCampaign));
            when(userService.getAccountIDByEmail(userEmail)).thenReturn(userId);
            when(campaignParticipantsRepository.existsByAccountIdAndCampaignId(userId, campaignId)).thenReturn(false);

            ResponseEntity<CreateCampaignSlotForUserResponse> response =
                    participantService.createCampaignSlotForUser(new CreateCampaignSlotForUserRequest(campaignId), userEmail);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Should return 503 SERVICE_UNAVAILABLE when no slots remain in campaign")
        void shouldReturn503WhenNoSlotsAvailable() {
            StoreCampaignToSlotHashMap.campaignToSlotStore.get(campaignId).set(0); // 0 slots

            when(campaignService.getCampaignById(campaignId)).thenReturn(Optional.of(approvedCampaign));
            when(userService.getAccountIDByEmail(userEmail)).thenReturn(userId);
            when(campaignParticipantsRepository.existsByAccountIdAndCampaignId(userId, campaignId)).thenReturn(false);

            ResponseEntity<CreateCampaignSlotForUserResponse> response =
                    participantService.createCampaignSlotForUser(new CreateCampaignSlotForUserRequest(campaignId), userEmail);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Nested
    @DisplayName("fetchOngoingUserSlotInfo and cancelReservedSlot Tests")
    class OngoingSlotAndCancelTests {

        @Test
        @DisplayName("fetchOngoingUserSlotInfo should return hasOngoingSlot=false when no slot exists")
        void fetchOngoing_ShouldReturnFalseWhenNoSlot() {
            ResponseEntity<FetchOnGoingSlotInformationForUserResponse> response =
                    participantService.fetchOngoingUserSlotInfo(userEmail);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().hasReservedSlot()).isFalse();
        }

        @Test
        @DisplayName("fetchOngoingUserSlotInfo should return full slot details when active")
        void fetchOngoing_ShouldReturnSlotDetailsWhenActive() {
            UUID slotKey = UUID.randomUUID();
            userEmailToSlotAvailableCheckCache.put(userEmail, slotKey);
            slotKeyToCampaignIdCache.put(slotKey, SlotKeyToCampaignAndUserIdDto.returnSlotKeyToCampaignAndUserIdDtoWithTime(campaignId, userEmail));

            when(campaignService.getCampaignById(campaignId)).thenReturn(Optional.of(approvedCampaign));

            ResponseEntity<FetchOnGoingSlotInformationForUserResponse> response =
                    participantService.fetchOngoingUserSlotInfo(userEmail);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().hasReservedSlot()).isTrue();
            assertThat(response.getBody().campaignId()).isEqualTo(campaignId);
            assertThat(response.getBody().slotKey()).isEqualTo(slotKey);
            assertThat(response.getBody().nameOfAccountHolder()).isEqualTo("Host Creator");
        }

        @Test
        @DisplayName("cancelReservedSlot should return 404 NOT_FOUND when user has no active slot")
        void cancelReservedSlot_ShouldReturnNotFound() {
            ResponseEntity<HttpStatus> response = participantService.cancelReservedSlot(userEmail);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("cancelReservedSlot should flag slot as cancelled in cache and return 200 OK")
        void cancelReservedSlot_ShouldFlagAsCancelled() {
            UUID slotKey = UUID.randomUUID();
            userEmailToSlotAvailableCheckCache.put(userEmail, slotKey);
            slotKeyToCampaignIdCache.put(slotKey, SlotKeyToCampaignAndUserIdDto.returnSlotKeyToCampaignAndUserIdDtoWithTime(campaignId, userEmail));

            ResponseEntity<HttpStatus> response = participantService.cancelReservedSlot(userEmail);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            SlotKeyToCampaignAndUserIdDto updatedDto = slotKeyToCampaignIdCache.getIfPresent(slotKey);
            assertThat(updatedDto).isNotNull();
            assertThat(updatedDto.flaggedForCanceledSlot()).isTrue();
        }
    }
}
