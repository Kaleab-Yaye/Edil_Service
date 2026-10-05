package com.edil.controller;

import com.edil.config.SecurityConfig;
import com.edil.domain.Account;
import com.edil.dto.request.*;
import com.edil.dto.response.*;
import com.edil.exception.GlobalExceptionHandler;
import com.edil.repository.AccountRepository;
import com.edil.security.JwtAuthenticationFilter;
import com.edil.security.JwtService;
import com.edil.service.CampaignService;
import com.edil.service.CreatorsCampaignService;
import com.edil.util.ArchiveCampaignAndGeneratePdfUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CreatorCampaignController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class CreatorCampaignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CampaignService campaignService;

    @MockBean
    private CreatorsCampaignService creatorsCampaignService;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private ArchiveCampaignAndGeneratePdfUtil archiveCampaignAndGeneratePdfUtil;

    @MockBean
    private JwtService jwtService;

    @Nested
    @DisplayName("Creator Role Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should reject unauthenticated requests with 403 Forbidden")
        void unauthenticated_Denied() throws Exception {
            mockMvc.perform(get("/api/creator/campaigns"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Should reject USER role with 401 Unauthorized")
        void userRole_Denied() throws Exception {
            mockMvc.perform(get("/api/creator/campaigns"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Creator Campaign Operations")
    class OperationsTests {

        @Test
        @WithMockUser(username = "creator@example.com", roles = {"CREATOR"})
        @DisplayName("POST /api/creator/campaigns - Should create campaign successfully")
        void createCampaign_Success() throws Exception {
            CreatePrizeRequest prize = CreatePrizeRequest.builder()
                    .title("Car")
                    .description("Brand new car")
                    .prizeOrder(1)
                    .imageFileId("img-123")
                    .build();

            CreateCampaignRequest request = CreateCampaignRequest.builder()
                    .title("Mega Raffle")
                    .aboutCampaign("Great prizes")
                    .ticketPrice(BigDecimal.valueOf(100))
                    .targetEntries(1000)
                    .startDate(LocalDateTime.now().plusDays(1))
                    .endDate(LocalDateTime.now().plusDays(10))
                    .prizes(List.of(prize))
                    .build();

            CampaignResponse response = CampaignResponse.builder()
                    .id(UUID.randomUUID())
                    .title("Mega Raffle")
                    .ticketPrice(BigDecimal.valueOf(100))
                    .status("PENDING")
                    .build();

            when(campaignService.createCampaign(eq("creator@example.com"), any(CreateCampaignRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/creator/campaigns")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Mega Raffle"))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @WithMockUser(username = "creator@example.com", roles = {"CREATOR"})
        @DisplayName("POST /api/creator/campaigns - Should return 400 on invalid payload")
        void createCampaign_InvalidPayload() throws Exception {
            CreateCampaignRequest request = CreateCampaignRequest.builder()
                    .title("")
                    .ticketPrice(BigDecimal.valueOf(-10))
                    .targetEntries(-5)
                    .build();

            mockMvc.perform(post("/api/creator/campaigns")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "creator@example.com", roles = {"CREATOR"})
        @DisplayName("GET /api/creator/campaigns - Should return creator campaigns")
        void getCreatorCampaigns_Success() throws Exception {
            UUID accountId = UUID.randomUUID();
            Account account = Account.builder().id(accountId).email("creator@example.com").build();
            CampaignResponse campaign = CampaignResponse.builder()
                    .id(UUID.randomUUID())
                    .title("Creator Campaign 1")
                    .ticketPrice(BigDecimal.valueOf(50))
                    .build();

            when(accountRepository.findByEmail("creator@example.com")).thenReturn(Optional.of(account));
            when(campaignService.getCampaignsByCreatorId(accountId)).thenReturn(List.of(campaign));

            mockMvc.perform(get("/api/creator/campaigns"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Creator Campaign 1"));
        }

        @Test
        @WithMockUser(username = "creator@example.com", roles = {"CREATOR"})
        @DisplayName("GET /api/creator/campaigns - Should return 404 if creator account not found")
        void getCreatorCampaigns_AccountNotFound() throws Exception {
            when(accountRepository.findByEmail("creator@example.com")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/creator/campaigns"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_FOUND"));
        }

        @Test
        @WithMockUser(username = "creator@example.com", roles = {"CREATOR"})
        @DisplayName("POST /api/creator/campaigns/end/campaign - Should request campaign ending")
        void endCampaign_Success() throws Exception {
            UUID campaignId = UUID.randomUUID();
            EndCampaignByCreatorRequest request = new EndCampaignByCreatorRequest(campaignId);
            EndCampaignByCreatorResponse response = new EndCampaignByCreatorResponse("Campaign end requested successfully");

            when(creatorsCampaignService.endCampaignByCreator(any(EndCampaignByCreatorRequest.class), any()))
                    .thenReturn(ResponseEntity.ok(response));

            mockMvc.perform(post("/api/creator/campaigns/end/campaign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Campaign end requested successfully"));
        }

        @Test
        @WithMockUser(username = "creator@example.com", roles = {"CREATOR"})
        @DisplayName("GET /api/creator/campaigns/report - Should return PDF report info")
        void getPdfInfo_Success() throws Exception {
            UUID campaignId = UUID.randomUUID();
            GetPdfInfoResponse response = new GetPdfInfoResponse("report.pdf", 1024L, true);

            when(creatorsCampaignService.getPdfForCreator(any(GetPdfInfoRequest.class), any()))
                    .thenReturn(ResponseEntity.ok(response));

            mockMvc.perform(get("/api/creator/campaigns/report")
                            .param("id", campaignId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pdfName").value("report.pdf"))
                    .andExpect(jsonPath("$.pdfSize").value(1024))
                    .andExpect(jsonPath("$.hasPdf").value(true));
        }

        @Test
        @WithMockUser(username = "creator@example.com", roles = {"CREATOR"})
        @DisplayName("GET /api/creator/campaigns/report/download/key - Should return download key")
        void getDownloadPdfKey_Success() throws Exception {
            UUID campaignId = UUID.randomUUID();
            UUID downloadKey = UUID.randomUUID();
            GetDownloadPdfKeyResponse response = new GetDownloadPdfKeyResponse(downloadKey);

            when(creatorsCampaignService.getPdfDownloadKey(any(GetDownloadPdfKeyRequest.class), any()))
                    .thenReturn(ResponseEntity.ok(response));

            mockMvc.perform(get("/api/creator/campaigns/report/download/key")
                            .param("id", campaignId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.downloadPdfKey").value(downloadKey.toString()));
        }

        @Test
        @WithMockUser(username = "creator@example.com", roles = {"CREATOR"})
        @DisplayName("POST /api/creator/campaigns/joined/user - Should return player info by edil number")
        void getJoinedInfoWithEdilNumber_Success() throws Exception {
            UUID campaignId = UUID.randomUUID();
            GetJoinedPlayerInfoWithEdilNumberRequest request = new GetJoinedPlayerInfoWithEdilNumberRequest(campaignId, "EDIL-12345");
            GetJoinedPlayerInfoWithEdilNumberResponse response = new GetJoinedPlayerInfoWithEdilNumberResponse(
                    true,
                    "Participant Name",
                    "0911002233"
            );

            when(creatorsCampaignService.getJoinedPlayerWithEdilNumber(any(GetJoinedPlayerInfoWithEdilNumberRequest.class), any()))
                    .thenReturn(ResponseEntity.ok(response));

            mockMvc.perform(post("/api/creator/campaigns/joined/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.presentInCampaign").value(true))
                    .andExpect(jsonPath("$.fullName").value("Participant Name"))
                    .andExpect(jsonPath("$.phoneNumber").value("0911002233"));
        }

        @Test
        @WithMockUser(username = "creator@example.com", roles = {"CREATOR"})
        @DisplayName("POST /api/creator/campaigns/joined/user - Should return 400 on invalid payload")
        void getJoinedInfoWithEdilNumber_InvalidPayload() throws Exception {
            GetJoinedPlayerInfoWithEdilNumberRequest request = new GetJoinedPlayerInfoWithEdilNumberRequest(null, "");

            mockMvc.perform(post("/api/creator/campaigns/joined/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
