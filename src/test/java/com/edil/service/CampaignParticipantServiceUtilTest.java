package com.edil.service;

import com.edil.domain.ArchivedCampaignParticipants;
import com.edil.domain.Campaign;
import com.edil.domain.CampaignParticipant;
import com.edil.domain.enums.CampaignStatus;
import com.edil.repository.ArchivedCampaignParticipantsRepository;
import com.edil.repository.CampaignParticipantsRepository;
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
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignParticipantServiceUtilTest {

    @Mock
    private RestClient restClient;

    @Mock
    private CampaignParticipantsRepository campaignParticipantsRepository;

    @Mock
    private ArchivedCampaignParticipantsRepository archivedCampaignParticipantsRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @InjectMocks
    private CampaignParticipantServiceUtil util;

    private Campaign endedCampaign;
    private Campaign activeCampaign;

    @BeforeEach
    void setUp() {
        endedCampaign = Campaign.builder()
                .id(UUID.randomUUID())
                .status(CampaignStatus.ENDED)
                .build();

        activeCampaign = Campaign.builder()
                .id(UUID.randomUUID())
                .status(CampaignStatus.APPROVED)
                .build();
    }

    @Nested
    @DisplayName("extractV2KeyFromCBeLink Tests")
    class ExtractV2KeyTests {

        @Test
        @DisplayName("Should extract key from valid CBE URL")
        void shouldExtractKeyFromValidCbeLink() {
            String url = "https://mbreciept.cbe.com.et/v2-TXN123456789";
            String key = util.extractV2KeyFromCBeLink(url);

            assertThat(key).isEqualTo("TXN123456789");
        }

        @Test
        @DisplayName("Should extract key with mixed alphanumeric characters")
        void shouldExtractMixedAlphaNumericKey() {
            String url = "https://mbreciept.cbe.com.et/v2-9A8b7C6d5E";
            String key = util.extractV2KeyFromCBeLink(url);

            assertThat(key).isEqualTo("9A8b7C6d5E");
        }

        @Test
        @DisplayName("Should return null for non-https protocol")
        void shouldReturnNullForHttpProtocol() {
            String url = "http://mbreciept.cbe.com.et/v2-TXN123456789";
            assertThat(util.extractV2KeyFromCBeLink(url)).isNull();
        }

        @Test
        @DisplayName("Should return null for wrong domain")
        void shouldReturnNullForWrongDomain() {
            String url = "https://fakephishing.cbe.com.et/v2-TXN123456789";
            assertThat(util.extractV2KeyFromCBeLink(url)).isNull();
        }

        @Test
        @DisplayName("Should return null for invalid format or missing v2- prefix")
        void shouldReturnNullForMissingV2Prefix() {
            String url = "https://mbreciept.cbe.com.et/v1-TXN123456789";
            assertThat(util.extractV2KeyFromCBeLink(url)).isNull();
        }

        @Test
        @DisplayName("Should return null for special characters in key")
        void shouldReturnNullForSpecialCharacters() {
            String url = "https://mbreciept.cbe.com.et/v2-TXN_123$";
            assertThat(util.extractV2KeyFromCBeLink(url)).isNull();
        }

        @Test
        @DisplayName("Should return null for empty string or null")
        void shouldReturnNullForEmptyOrNull() {
            assertThat(util.extractV2KeyFromCBeLink("")).isNull();
        }
    }

    @Nested
    @DisplayName("generateRandomPassword Tests")
    class GeneratePasswordTests {

        @Test
        @DisplayName("Should generate password of specified length")
        void shouldGeneratePasswordWithExactLength() {
            String password12 = util.generateRandomPassword(12);
            assertThat(password12).hasSize(12);

            String password16 = util.generateRandomPassword(16);
            assertThat(password16).hasSize(16);
        }

        @Test
        @DisplayName("Generated password should meet complexity rules (uppercase, lowercase, digit)")
        void shouldMeetComplexityRules() {
            for (int i = 0; i < 10; i++) {
                String password = util.generateRandomPassword(14);
                assertThat(password).matches(".*[A-Z].*"); // at least one uppercase
                assertThat(password).matches(".*[a-z].*"); // at least one lowercase
                assertThat(password).matches(".*[0-9].*"); // at least one digit
            }
        }
    }

    @Nested
    @DisplayName("addCampaignParticipantWithGeneratedRandomLotteryNumber Tests")
    class LotteryNumberTests {

        @Test
        @DisplayName("Should generate lottery number in XXX-XXX-XXX format and save participant")
        void shouldGenerateFormattedLotteryNumber() {
            CampaignParticipant participant = new CampaignParticipant();
            when(campaignParticipantsRepository.existsByEdilCode(anyString())).thenReturn(false);

            String edilCode = util.addCampaignParticipantWithGeneratedRandomLotteryNumber(participant);

            assertThat(edilCode).isNotBlank();
            assertThat(edilCode).matches("^\\d{3}-\\d{3}-\\d{3}$");
            verify(campaignParticipantsRepository).save(participant);
            assertThat(participant.getEdilCode()).isNotBlank();
        }

        @Test
        @DisplayName("Should retry generation if first code already exists in repository")
        void shouldRetryWhenCollisionOccurs() {
            CampaignParticipant participant = new CampaignParticipant();
            // First attempt collides, second attempt succeeds
            when(campaignParticipantsRepository.existsByEdilCode(anyString()))
                    .thenReturn(true)
                    .thenReturn(false);

            String edilCode = util.addCampaignParticipantWithGeneratedRandomLotteryNumber(participant);

            assertThat(edilCode).isNotBlank();
            verify(campaignParticipantsRepository, atLeast(2)).existsByEdilCode(anyString());
            verify(campaignParticipantsRepository).save(participant);
        }
    }

    @Nested
    @DisplayName("archiveParticipantsOfAnEndedCampaign Tests")
    class ArchiveParticipantsTests {

        @Test
        @DisplayName("Should not archive participants if campaign status is NOT ENDED")
        void shouldNotArchiveWhenNotEnded() {
            util.archiveParticipantsOfAnEndedCampaign(activeCampaign);

            verify(campaignParticipantsRepository, never()).findCampaignParticipantsByCampaign(any());
            verify(archivedCampaignParticipantsRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should archive and delete all participants when campaign status is ENDED")
        void shouldArchiveParticipantsWhenEnded() {
            CampaignParticipant p1 = new CampaignParticipant();
            p1.setId(UUID.randomUUID());
            p1.setEdilCode("123-456-789");
            p1.setReceiptHash("hash1");
            p1.setCampaign(endedCampaign);

            when(campaignParticipantsRepository.findCampaignParticipantsByCampaign(endedCampaign))
                    .thenReturn(List.of(p1));

            util.archiveParticipantsOfAnEndedCampaign(endedCampaign);

            verify(archivedCampaignParticipantsRepository).save(any(ArchivedCampaignParticipants.class));
            verify(campaignParticipantsRepository).delete(p1);
        }

        @Test
        @DisplayName("Should archive participants by campaign ID")
        void shouldArchiveParticipantsByCampaignId() {
            UUID id = endedCampaign.getId();
            when(campaignRepository.findById(id)).thenReturn(Optional.of(endedCampaign));
            when(campaignParticipantsRepository.findCampaignParticipantsByCampaign(endedCampaign))
                    .thenReturn(Collections.emptyList());

            util.archiveParticipantsOfAnEndedCampaign(id);

            verify(campaignRepository).findById(id);
            verify(campaignParticipantsRepository).findCampaignParticipantsByCampaign(endedCampaign);
        }
    }
}
