package com.edil.service;

import com.edil.domain.Account;
import com.edil.domain.ArchivedCampaignParticipants;
import com.edil.domain.Campaign;
import com.edil.domain.CampaignParticipant;
import com.edil.domain.CampaignParticipantsPdf;
import com.edil.domain.UserProfile;
import com.edil.domain.enums.CampaignStatus;
import com.edil.dto.request.EndCampaignByCreatorRequest;
import com.edil.dto.request.GetDownloadPdfKeyRequest;
import com.edil.dto.request.GetJoinedPlayerInfoWithEdilNumberRequest;
import com.edil.dto.request.GetPdfInfoRequest;
import com.edil.dto.response.EndCampaignByCreatorResponse;
import com.edil.dto.response.GetDownloadPdfKeyResponse;
import com.edil.dto.response.GetJoinedPlayerInfoWithEdilNumberResponse;
import com.edil.dto.response.GetPdfInfoResponse;
import com.edil.exception.CampaignNotFoundException;
import com.edil.repository.ArchivedCampaignParticipantsRepository;
import com.edil.repository.CampaignParticipantsRepository;
import com.edil.repository.CampaignRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatorsCampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private UserService userService;

    @Mock
    private CampaignParticipantsRepository campaignParticipantRepo;

    @Mock
    private ArchivedCampaignParticipantsRepository archivedCampaignParticipantsRepo;

    private Cache<UUID, String> uploadPdfKeyToPdfNameCache;
    private CreatorsCampaignService creatorsCampaignService;

    private UUID campaignId;
    private UUID creatorId;
    private Account creatorAccount;
    private Campaign approvedCampaign;

    @BeforeEach
    void setUp() {
        uploadPdfKeyToPdfNameCache = Caffeine.newBuilder().build();
        creatorsCampaignService = new CreatorsCampaignService(
                campaignRepository,
                userService,
                uploadPdfKeyToPdfNameCache,
                campaignParticipantRepo,
                archivedCampaignParticipantsRepo
        );

        campaignId = UUID.randomUUID();
        creatorId = UUID.randomUUID();

        creatorAccount = Account.builder()
                .id(creatorId)
                .email("creator@edil.com")
                .build();

        approvedCampaign = Campaign.builder()
                .id(campaignId)
                .creator(creatorAccount)
                .status(CampaignStatus.APPROVED)
                .hasPdf(false)
                .build();
    }

    @Nested
    @DisplayName("endCampaignByCreator Tests")
    class EndCampaignTests {

        @Test
        @DisplayName("Should successfully end campaign when caller is the owner and status is APPROVED")
        void shouldEndCampaignSuccessfully() {
            when(campaignRepository.getCampaignsById(campaignId)).thenReturn(Optional.of(approvedCampaign));
            when(userService.getAccountByEmail("creator@edil.com")).thenReturn(creatorAccount);

            ResponseEntity<EndCampaignByCreatorResponse> response =
                    creatorsCampaignService.endCampaignByCreator(new EndCampaignByCreatorRequest(campaignId), "creator@edil.com");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).contains("status will be updated");
            assertThat(approvedCampaign.getStatus()).isEqualTo(CampaignStatus.ENDED_BY_CREATOR);
            verify(campaignRepository).save(approvedCampaign);
        }

        @Test
        @DisplayName("Should return 401 UNAUTHORIZED if caller is not the campaign owner")
        void shouldRejectNonOwner() {
            Account imposterAccount = Account.builder().id(UUID.randomUUID()).email("imposter@edil.com").build();

            when(campaignRepository.getCampaignsById(campaignId)).thenReturn(Optional.of(approvedCampaign));
            when(userService.getAccountByEmail("imposter@edil.com")).thenReturn(imposterAccount);

            ResponseEntity<EndCampaignByCreatorResponse> response =
                    creatorsCampaignService.endCampaignByCreator(new EndCampaignByCreatorRequest(campaignId), "imposter@edil.com");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).contains("not allowed to change state");
            verify(campaignRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should return 406 NOT_ACCEPTABLE if campaign status is not APPROVED")
        void shouldRejectNonApprovedCampaign() {
            approvedCampaign.setStatus(CampaignStatus.PENDING);

            when(campaignRepository.getCampaignsById(campaignId)).thenReturn(Optional.of(approvedCampaign));
            when(userService.getAccountByEmail("creator@edil.com")).thenReturn(creatorAccount);

            ResponseEntity<EndCampaignByCreatorResponse> response =
                    creatorsCampaignService.endCampaignByCreator(new EndCampaignByCreatorRequest(campaignId), "creator@edil.com");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).contains("current status of campaign can't be ended");
        }
    }

    @Nested
    @DisplayName("PDF Info and Download Key Tests")
    class PdfTests {

        @Test
        @DisplayName("getPdfForCreator should return PDF info when campaign is ENDED and has PDF")
        void shouldReturnPdfInfoWhenEnded() {
            CampaignParticipantsPdf pdf = new CampaignParticipantsPdf();
            pdf.setPdfName("participants-report.pdf");
            pdf.setPdfSizeInBytes(1024L);

            Campaign endedCampaign = Campaign.builder()
                    .id(campaignId)
                    .creator(creatorAccount)
                    .status(CampaignStatus.ENDED)
                    .hasPdf(true)
                    .campaignParticipantsPdf(pdf)
                    .build();

            when(campaignRepository.getCampaignsById(campaignId)).thenReturn(Optional.of(endedCampaign));

            ResponseEntity<GetPdfInfoResponse> response =
                    creatorsCampaignService.getPdfForCreator(new GetPdfInfoRequest(campaignId), "creator@edil.com");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().pdfName()).isEqualTo("participants-report.pdf");
            assertThat(response.getBody().pdfSize()).isEqualTo(1024L);
            assertThat(response.getBody().hasPdf()).isTrue();
        }

        @Test
        @DisplayName("getPdfForCreator should return 403 FORBIDDEN if campaign has not ended")
        void shouldRejectPdfInfoWhenNotEnded() {
            when(campaignRepository.getCampaignsById(campaignId)).thenReturn(Optional.of(approvedCampaign));

            ResponseEntity<GetPdfInfoResponse> response =
                    creatorsCampaignService.getPdfForCreator(new GetPdfInfoRequest(campaignId), "creator@edil.com");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("getPdfDownloadKey should generate key and store in cache")
        void shouldGenerateDownloadKey() {
            CampaignParticipantsPdf pdf = new CampaignParticipantsPdf();
            pdf.setPdfName("official-report.pdf");

            Campaign endedCampaign = Campaign.builder()
                    .id(campaignId)
                    .creator(creatorAccount)
                    .status(CampaignStatus.ENDED)
                    .hasPdf(true)
                    .campaignParticipantsPdf(pdf)
                    .build();

            when(campaignRepository.getCampaignsById(campaignId)).thenReturn(Optional.of(endedCampaign));

            ResponseEntity<GetDownloadPdfKeyResponse> response =
                    creatorsCampaignService.getPdfDownloadKey(new GetDownloadPdfKeyRequest(campaignId), "creator@edil.com");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            UUID key = response.getBody().downloadPdfKey();
            assertThat(key).isNotNull();
            assertThat(uploadPdfKeyToPdfNameCache.getIfPresent(key)).isEqualTo("official-report.pdf");
        }

        @Test
        @DisplayName("canDownloadPdf should validate request URL against cached PDF name")
        void shouldValidatePdfDownloadUrl() {
            UUID key = UUID.randomUUID();
            uploadPdfKeyToPdfNameCache.put(key, "report123.pdf");

            String validUrl = "/download/report/pdf/report123.pdf?key=" + key;
            boolean canDownload = creatorsCampaignService.canDownloadPdf(key, validUrl);
            assertThat(canDownload).isTrue();

            String mismatchUrl = "/download/report/pdf/wrong-report.pdf?key=" + key;
            boolean cannotDownload = creatorsCampaignService.canDownloadPdf(key, mismatchUrl);
            assertThat(cannotDownload).isFalse();

            boolean invalidKey = creatorsCampaignService.canDownloadPdf(UUID.randomUUID(), validUrl);
            assertThat(invalidKey).isFalse();
        }
    }

    @Nested
    @DisplayName("getJoinedPlayerWithEdilNumber Tests")
    class JoinedPlayerLookupTests {

        @Test
        @DisplayName("Should lookup player from active participants when campaign is APPROVED")
        void shouldLookupPlayerFromActiveParticipants() {
            UserProfile profile = UserProfile.builder()
                    .fullName("Tadesse Gebre")
                    .phoneNumber("+251911332211")
                    .build();

            Account playerAccount = Account.builder().userProfile(profile).build();

            CampaignParticipant participant = new CampaignParticipant();
            participant.setAccount(playerAccount);
            participant.setEdilCode("123-456-789");

            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(approvedCampaign));
            when(campaignParticipantRepo.findCampaignParticipantsByEdilCodeAndCampaignId("123-456-789", campaignId))
                    .thenReturn(Optional.of(participant));

            GetJoinedPlayerInfoWithEdilNumberRequest request =
                    new GetJoinedPlayerInfoWithEdilNumberRequest(campaignId, "123-456-789");

            ResponseEntity<GetJoinedPlayerInfoWithEdilNumberResponse> response =
                    creatorsCampaignService.getJoinedPlayerWithEdilNumber(request, "creator@edil.com");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().presentInCampaign()).isTrue();
            assertThat(response.getBody().fullName()).isEqualTo("Tadesse Gebre");
            assertThat(response.getBody().phoneNumber()).isEqualTo("+251911332211");
        }

        @Test
        @DisplayName("Should lookup player from archived participants when campaign is ENDED")
        void shouldLookupPlayerFromArchivedParticipants() {
            Campaign endedCampaign = Campaign.builder()
                    .id(campaignId)
                    .creator(creatorAccount)
                    .status(CampaignStatus.ENDED)
                    .build();

            UserProfile profile = UserProfile.builder()
                    .fullName("Bethlehem Tilahun")
                    .phoneNumber("+251922554433")
                    .build();

            Account playerAccount = Account.builder().userProfile(profile).build();

            ArchivedCampaignParticipants archived = new ArchivedCampaignParticipants();
            archived.setAccount(playerAccount);
            archived.setEdilCode("987-654-321");

            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(endedCampaign));
            when(archivedCampaignParticipantsRepo.findArchivedCampaignParticipantsByEdilCodeAndCampaignId("987-654-321", campaignId))
                    .thenReturn(Optional.of(archived));

            GetJoinedPlayerInfoWithEdilNumberRequest request =
                    new GetJoinedPlayerInfoWithEdilNumberRequest(campaignId, "987-654-321");

            ResponseEntity<GetJoinedPlayerInfoWithEdilNumberResponse> response =
                    creatorsCampaignService.getJoinedPlayerWithEdilNumber(request, "creator@edil.com");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().presentInCampaign()).isTrue();
            assertThat(response.getBody().fullName()).isEqualTo("Bethlehem Tilahun");
            assertThat(response.getBody().phoneNumber()).isEqualTo("+251922554433");
        }

        @Test
        @DisplayName("Should return 403 FORBIDDEN when non-creator attempts to lookup player")
        void shouldRejectNonCreatorLookup() {
            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(approvedCampaign));

            GetJoinedPlayerInfoWithEdilNumberRequest request =
                    new GetJoinedPlayerInfoWithEdilNumberRequest(campaignId, "123-456-789");

            ResponseEntity<GetJoinedPlayerInfoWithEdilNumberResponse> response =
                    creatorsCampaignService.getJoinedPlayerWithEdilNumber(request, "outsider@edil.com");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}
