package com.edil.service;

import com.edil.domain.Account;
import com.edil.domain.AdminProfile;
import com.edil.domain.Campaign;
import com.edil.domain.Petition;
import com.edil.domain.UserProfile;
import com.edil.domain.enums.AccountRole;
import com.edil.domain.enums.PetitionReason;
import com.edil.domain.enums.PetitionStatus;
import com.edil.dto.request.CreatePetitionRequest;
import com.edil.dto.request.HandlePetitionRequest;
import com.edil.dto.request.ResolvePetitionRequest;
import com.edil.dto.response.CreatePetitionResponse;
import com.edil.dto.response.GetPetitionsBeingHandledByMeResponse;
import com.edil.dto.response.GetUnresolvedPetitionsResponse;
import com.edil.dto.response.HandlePetitionResponse;
import com.edil.dto.response.ResolvePetitionResponse;
import com.edil.exception.ResourceNotFoundException;
import com.edil.repository.AdminProfileRepository;
import com.edil.repository.PetitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PetitionServiceTest {

    @Mock
    private PetitionRepository petitionRepository;

    @Mock
    private UserService userService;

    @Mock
    private AdminProfileRepository adminProfileRepository;

    @InjectMocks
    private PetitionService petitionService;

    private UUID campaignId;
    private UUID userId;
    private UserProfile userProfile;
    private Campaign campaign;
    private CreatePetitionRequest createPetitionRequest;

    @BeforeEach
    void setUp() {
        campaignId = UUID.randomUUID();
        userId = UUID.randomUUID();

        Account userAccount = Account.builder()
                .id(UUID.randomUUID())
                .email("user@edil.com")
                .role(AccountRole.USER)
                .isActive(true)
                .build();

        userProfile = UserProfile.builder()
                .id(userId)
                .account(userAccount)
                .fullName("Solomon Tesfaye")
                .phoneNumber("+251911445566")
                .build();

        campaign = Campaign.builder()
                .id(campaignId)
                .title("Mega Raffle")
                .build();

        createPetitionRequest = new CreatePetitionRequest(
                "https://mbreciept.cbe.com.et/v2-petition123",
                PetitionReason.SLOT_EXPIRED_DURING_TRANSFER,
                "Payment was deducted from my account but slot expired",
                campaignId
        );
    }

    @Test
    @DisplayName("submitPetition should succeed and return 200 OK when no ongoing petition exists")
    void submitPetition_ShouldSucceedWhenNoOngoingPetition() {
        when(petitionRepository.existsByPetitionerIdAndCampaignIdAndStatus(userId, campaignId, PetitionStatus.UNRESOLVED))
                .thenReturn(false);

        ResponseEntity<CreatePetitionResponse> response = petitionService.submitPetition(createPetitionRequest, userProfile, campaign);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Petition Submitted");

        ArgumentCaptor<Petition> petitionCaptor = ArgumentCaptor.forClass(Petition.class);
        verify(petitionRepository).save(petitionCaptor.capture());
        Petition savedPetition = petitionCaptor.getValue();
        assertThat(savedPetition.getCampaign()).isEqualTo(campaign);
        assertThat(savedPetition.getPetitioner()).isEqualTo(userProfile);
        assertThat(savedPetition.getReason()).isEqualTo(PetitionReason.SLOT_EXPIRED_DURING_TRANSFER);
        assertThat(savedPetition.getPaymentLink()).isEqualTo("https://mbreciept.cbe.com.et/v2-petition123");
        assertThat(savedPetition.getStatement()).isEqualTo("Payment was deducted from my account but slot expired");
    }

    @Test
    @DisplayName("submitPetition should return 409 CONFLICT if user already has unresolved petition for campaign")
    void submitPetition_ShouldReturnConflictWhenOngoingPetitionExists() {
        when(petitionRepository.existsByPetitionerIdAndCampaignIdAndStatus(userId, campaignId, PetitionStatus.UNRESOLVED))
                .thenReturn(true);

        ResponseEntity<CreatePetitionResponse> response = petitionService.submitPetition(createPetitionRequest, userProfile, campaign);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("user already has ongoing petition for this campaign");

        verify(petitionRepository, never()).save(any());
    }

    @Test
    @DisplayName("doesUserHasOngoingPetitionForCampaign should delegate to repository")
    void doesUserHasOngoingPetitionForCampaign_ShouldDelegate() {
        when(petitionRepository.existsByPetitionerIdAndCampaignIdAndStatus(userId, campaignId, PetitionStatus.UNRESOLVED))
                .thenReturn(true);

        boolean exists = petitionService.doesUserHasOngoingPetitionForCampaign(campaignId, userId);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("getUnresolvedPetitions should return page of unresolved petitions")
    void getUnresolvedPetitions_ShouldReturnMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Petition petition = Petition.builder()
                .id(UUID.randomUUID())
                .reason(PetitionReason.SLOT_EXPIRED_DURING_TRANSFER)
                .paymentLink("https://mbreciept.cbe.com.et/v2-link1")
                .createdAt(LocalDateTime.now())
                .petitioner(userProfile)
                .campaign(campaign)
                .status(PetitionStatus.UNRESOLVED)
                .build();

        when(petitionRepository.getPetitionsByStatus(PetitionStatus.UNRESOLVED, pageable))
                .thenReturn(new PageImpl<>(List.of(petition)));

        ResponseEntity<Page<GetUnresolvedPetitionsResponse>> response = petitionService.getUnresolvedPetitions(pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        GetUnresolvedPetitionsResponse dto = response.getBody().getContent().get(0);
        assertThat(dto.id()).isEqualTo(petition.getId());
        assertThat(dto.petitionerId()).isEqualTo(userId);
        assertThat(dto.campaignId()).isEqualTo(campaignId);
    }

    @Test
    @DisplayName("handlePetition should allow admin to take ownership of UNRESOLVED petition")
    void handlePetition_ShouldSucceedForAdmin() {
        UUID petitionId = UUID.randomUUID();
        Petition petition = Petition.builder()
                .id(petitionId)
                .status(PetitionStatus.UNRESOLVED)
                .build();

        AdminProfile adminProfile = AdminProfile.builder().id(UUID.randomUUID()).build();
        Account adminAccount = Account.builder()
                .id(UUID.randomUUID())
                .email("admin@edil.com")
                .role(AccountRole.ADMIN)
                .adminProfile(adminProfile)
                .build();

        when(petitionRepository.getPetitionsById(petitionId)).thenReturn(Optional.of(petition));
        when(userService.getAccountByEmail("admin@edil.com")).thenReturn(adminAccount);

        ResponseEntity<HandlePetitionResponse> response = petitionService.handlePetition(
                new HandlePetitionRequest(petitionId), "admin@edil.com"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().becameHandler()).isTrue();
        assertThat(petition.getStatus()).isEqualTo(PetitionStatus.BING_HANDLED);
        assertThat(petition.getResolverAdmin()).isEqualTo(adminProfile);
        verify(petitionRepository).save(petition);
    }

    @Test
    @DisplayName("handlePetition should return 401 UNAUTHORIZED if non-admin tries to handle petition")
    void handlePetition_ShouldRejectNonAdmin() {
        UUID petitionId = UUID.randomUUID();
        Petition petition = Petition.builder()
                .id(petitionId)
                .status(PetitionStatus.UNRESOLVED)
                .build();

        Account nonAdmin = Account.builder()
                .id(UUID.randomUUID())
                .email("user@edil.com")
                .role(AccountRole.USER)
                .build();

        when(petitionRepository.getPetitionsById(petitionId)).thenReturn(Optional.of(petition));
        when(userService.getAccountByEmail("user@edil.com")).thenReturn(nonAdmin);

        ResponseEntity<HandlePetitionResponse> response = petitionService.handlePetition(
                new HandlePetitionRequest(petitionId), "user@edil.com"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(petitionRepository, never()).save(any());
    }

    @Test
    @DisplayName("handlePetition should return 208 ALREADY_REPORTED if petition is handled by another admin")
    void handlePetition_ShouldReturnAlreadyReportedWhenHandledByAnotherAdmin() {
        UUID petitionId = UUID.randomUUID();
        AdminProfile otherAdmin = AdminProfile.builder().id(UUID.randomUUID()).build();
        Petition petition = Petition.builder()
                .id(petitionId)
                .status(PetitionStatus.BING_HANDLED)
                .resolverAdmin(otherAdmin)
                .build();

        AdminProfile thisAdmin = AdminProfile.builder().id(UUID.randomUUID()).build();
        Account thisAccount = Account.builder()
                .id(UUID.randomUUID())
                .email("thisadmin@edil.com")
                .role(AccountRole.ADMIN)
                .adminProfile(thisAdmin)
                .build();

        when(petitionRepository.getPetitionsById(petitionId)).thenReturn(Optional.of(petition));
        when(userService.getAccountByEmail("thisadmin@edil.com")).thenReturn(thisAccount);

        ResponseEntity<HandlePetitionResponse> response = petitionService.handlePetition(
                new HandlePetitionRequest(petitionId), "thisadmin@edil.com"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ALREADY_REPORTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().becameHandler()).isFalse();
        verify(petitionRepository, never()).save(any());
    }

    @Test
    @DisplayName("resolvePetition should succeed when the handling admin resolves it")
    void resolvePetition_ShouldSucceedForHandlingAdmin() {
        UUID petitionId = UUID.randomUUID();
        Account adminAccount = Account.builder()
                .id(UUID.randomUUID())
                .email("resolver@edil.com")
                .build();

        AdminProfile adminProfile = AdminProfile.builder()
                .id(UUID.randomUUID())
                .account(adminAccount)
                .build();

        Petition petition = Petition.builder()
                .id(petitionId)
                .status(PetitionStatus.BING_HANDLED)
                .resolverAdmin(adminProfile)
                .build();

        when(petitionRepository.getPetitionsById(petitionId)).thenReturn(Optional.of(petition));

        ResolvePetitionRequest request = new ResolvePetitionRequest(petitionId, PetitionStatus.APPROVED);
        ResponseEntity<ResolvePetitionResponse> response = petitionService.resolvePetition(request, "resolver@edil.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().resolved()).isTrue();
        assertThat(response.getBody().message()).isEqualTo("done");
        assertThat(petition.getStatus()).isEqualTo(PetitionStatus.APPROVED);
        assertThat(petition.getResolvedAt()).isNotNull();
        verify(petitionRepository).save(petition);
    }

    @Test
    @DisplayName("resolvePetition should return 401 UNAUTHORIZED if different admin attempts to resolve")
    void resolvePetition_ShouldRejectDifferentAdmin() {
        UUID petitionId = UUID.randomUUID();
        Account originalAdminAccount = Account.builder()
                .email("original@edil.com")
                .build();

        AdminProfile originalAdminProfile = AdminProfile.builder()
                .account(originalAdminAccount)
                .build();

        Petition petition = Petition.builder()
                .id(petitionId)
                .status(PetitionStatus.BING_HANDLED)
                .resolverAdmin(originalAdminProfile)
                .build();

        when(petitionRepository.getPetitionsById(petitionId)).thenReturn(Optional.of(petition));

        ResolvePetitionRequest request = new ResolvePetitionRequest(petitionId, PetitionStatus.REJECTED);
        ResponseEntity<ResolvePetitionResponse> response = petitionService.resolvePetition(request, "imposter@edil.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().resolved()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("other admin is already handling the petition");
        verify(petitionRepository, never()).save(any());
    }

    @Test
    @DisplayName("resolvePetition should throw ResourceNotFoundException if petition does not exist")
    void resolvePetition_ShouldThrowWhenPetitionNotFound() {
        UUID missingId = UUID.randomUUID();
        when(petitionRepository.getPetitionsById(missingId)).thenReturn(Optional.empty());

        ResolvePetitionRequest request = new ResolvePetitionRequest(missingId, PetitionStatus.APPROVED);

        assertThatThrownBy(() -> petitionService.resolvePetition(request, "admin@edil.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(missingId.toString());
    }
}
