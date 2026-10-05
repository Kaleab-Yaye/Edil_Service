package com.edil.util;

import com.edil.domain.Account;
import com.edil.domain.ActiveCampaignPrize;
import com.edil.domain.ArchivedCampaignParticipants;
import com.edil.domain.ArchivedCampaignPrize;
import com.edil.domain.Campaign;
import com.edil.domain.CampaignParticipant;
import com.edil.domain.CampaignParticipantsPdf;
import com.edil.domain.UserProfile;
import com.edil.domain.enums.CampaignStatus;
import com.edil.repository.ArchivedCampaignParticipantsRepository;
import com.edil.repository.ArchivedCampaignPrizeRepository;
import com.edil.repository.CampaignParticipantPdfRepository;
import com.edil.repository.CampaignParticipantsRepository;
import com.edil.repository.CampaignRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArchiveCampaignAndGeneratePdfUtilTest {

    @Mock
    private ArchivedCampaignParticipantsRepository archivedCampaignParticipantsRepository;

    @Mock
    private ArchivedCampaignPrizeRepository archivedCampaignPrizeRepository;

    @Mock
    private CampaignParticipantPdfRepository campaignParticipantPdfRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignParticipantsRepository campaignParticipantsRepository;

    @InjectMocks
    private ArchiveCampaignAndGeneratePdfUtil pdfUtil;

    private UUID campaignId;
    private Campaign campaign;
    private List<String> createdPdfFiles;

    @BeforeEach
    void setUp() {
        campaignId = UUID.randomUUID();
        createdPdfFiles = new ArrayList<>();

        // Ensure target pdf directory exists
        new File("./pdf_store").mkdirs();

        LocalDateTime startDate = LocalDateTime.of(2026, 5, 10, 14, 30);
        LocalDateTime endDate = LocalDateTime.of(2026, 5, 20, 14, 30);

        ActiveCampaignPrize prize = ActiveCampaignPrize.builder()
                .id(UUID.randomUUID())
                .title("MacBook Pro M3")
                .description("16GB RAM 512GB SSD")
                .prizeOrder(1)
                .imageUrl("http://localhost:8081/static/macbook.jpg")
                .build();

        campaign = Campaign.builder()
                .id(campaignId)
                .title("Tech Extravaganza")
                .startDate(startDate)
                .endDate(endDate)
                .status(CampaignStatus.ENDED_BY_CREATOR)
                .activePrizes(List.of(prize))
                .campaignParticipant(new ArrayList<>())
                .build();
    }

    @AfterEach
    void tearDown() {
        for (String filePath : createdPdfFiles) {
            try {
                Files.deleteIfExists(Paths.get(filePath));
            } catch (IOException ignored) {
            }
        }
    }

    @Test
    @DisplayName("Should successfully archive prizes, generate participants PDF on disk, and update campaign status")
    void shouldArchiveAndGeneratePdfSuccessfully() {
        // Setup participant
        UserProfile profile = UserProfile.builder()
                .id(UUID.randomUUID())
                .fullName("Kassahun Desta")
                .phoneNumber("+251911889900")
                .refundBankAccount("100033445566")
                .address("Addis Ababa, Kazanchis")
                .build();

        Account participantAccount = Account.builder()
                .id(UUID.randomUUID())
                .email("kassahun@edil.com")
                .userProfile(profile)
                .build();

        CampaignParticipant participant = new CampaignParticipant();
        participant.setId(UUID.randomUUID());
        participant.setCampaign(campaign);
        participant.setAccount(participantAccount);
        participant.setReceiptHash("REC998877");
        participant.setEdilCode("100-200-300");

        campaign.getCampaignParticipant().add(participant);

        when(campaignRepository.getCampaignsById(campaignId)).thenReturn(Optional.of(campaign));

        AtomicInteger openSlot = new AtomicInteger(10);

        pdfUtil.handleArchivalAndGeneratePdfUtil(
                archivedCampaignParticipantsRepository,
                archivedCampaignPrizeRepository,
                campaignParticipantPdfRepository,
                campaignRepository,
                campaignParticipantsRepository,
                campaignId,
                openSlot
        );

        // 1. Verify prize archival
        verify(archivedCampaignPrizeRepository).saveAll(anyList());

        // 2. Verify participant archival and deletion
        verify(archivedCampaignParticipantsRepository).save(any(ArchivedCampaignParticipants.class));
        verify(campaignParticipantsRepository).delete(participant);

        // 3. Verify campaign status updated to ENDED_BY_CREATOR_PROCESSED and hasPdf set to true
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ENDED_BY_CREATOR_PROCESSED);
        assertThat(campaign.getHasPdf()).isTrue();
        verify(campaignRepository).save(campaign);

        // 4. Verify PDF entity was recorded
        ArgumentCaptor<CampaignParticipantsPdf> pdfCaptor = ArgumentCaptor.forClass(CampaignParticipantsPdf.class);
        verify(campaignParticipantPdfRepository).save(pdfCaptor.capture());
        CampaignParticipantsPdf savedPdf = pdfCaptor.getValue();
        assertThat(savedPdf.getPdfName()).contains("Tech-Extravaganza");
        assertThat(savedPdf.getPdfSizeInBytes()).isGreaterThan(0L);

        // 5. Verify PDF file was physically generated on disk
        String expectedPath = "./pdf_store/" + savedPdf.getPdfName();
        createdPdfFiles.add(expectedPath);
        Path pdfPath = Paths.get(expectedPath);
        assertThat(Files.exists(pdfPath)).isTrue();
        assertThat(pdfPath.toFile().length()).isGreaterThan(0L);

        // 6. Verify openSlot counter was incremented
        assertThat(openSlot.get()).isEqualTo(11);
    }

    @Test
    @DisplayName("Should archive prizes and return early without creating PDF when participants list is empty")
    void shouldReturnEarlyWhenNoParticipants() {
        campaign.setCampaignParticipant(Collections.emptyList());
        when(campaignRepository.getCampaignsById(campaignId)).thenReturn(Optional.of(campaign));

        AtomicInteger openSlot = new AtomicInteger(5);

        pdfUtil.handleArchivalAndGeneratePdfUtil(
                archivedCampaignParticipantsRepository,
                archivedCampaignPrizeRepository,
                campaignParticipantPdfRepository,
                campaignRepository,
                campaignParticipantsRepository,
                campaignId,
                openSlot
        );

        verify(archivedCampaignPrizeRepository).saveAll(anyList());
        verify(campaignParticipantPdfRepository, never()).save(any());
        verify(campaignParticipantsRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should increment openSlot and throw RuntimeException when campaign is not found")
    void shouldThrowAndIncrementSlotWhenCampaignNotFound() {
        when(campaignRepository.getCampaignsById(campaignId)).thenReturn(Optional.empty());

        AtomicInteger openSlot = new AtomicInteger(3);

        assertThatThrownBy(() -> pdfUtil.handleArchivalAndGeneratePdfUtil(
                archivedCampaignParticipantsRepository,
                archivedCampaignPrizeRepository,
                campaignParticipantPdfRepository,
                campaignRepository,
                campaignParticipantsRepository,
                campaignId,
                openSlot
        )).isInstanceOf(RuntimeException.class);

        // Slot must still be freed on failure to prevent resource leaks
        assertThat(openSlot.get()).isEqualTo(4);
    }
}
