package com.edil.controller;

import com.edil.config.SecurityConfig;
import com.edil.dto.response.CampaignResponse;
import com.edil.exception.CampaignNotFoundException;
import com.edil.exception.GlobalExceptionHandler;
import com.edil.repository.AccountRepository;
import com.edil.security.JwtAuthenticationFilter;
import com.edil.security.JwtService;
import com.edil.service.CampaignService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminCampaignController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class AdminCampaignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CampaignService campaignService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AccountRepository accountRepository;

    @Nested
    @DisplayName("Security Authorization Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should reject unauthenticated request with 403 Forbidden")
        void unauthenticated_Denied() throws Exception {
            mockMvc.perform(get("/api/admin/campaigns/pending"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Should reject USER role with 403 Forbidden")
        void userRole_Denied() throws Exception {
            mockMvc.perform(get("/api/admin/campaigns/pending"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "creator@example.com", roles = {"CREATOR"})
        @DisplayName("Should reject CREATOR role with 403 Forbidden")
        void creatorRole_Denied() throws Exception {
            mockMvc.perform(get("/api/admin/campaigns/pending"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Admin Endpoints Execution Tests")
    class AdminExecutionTests {

        @Test
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        @DisplayName("Should return pending campaigns for ADMIN")
        void getPendingCampaigns_Admin() throws Exception {
            CampaignResponse campaign = CampaignResponse.builder()
                    .id(UUID.randomUUID())
                    .title("Pending Campaign")
                    .ticketPrice(BigDecimal.valueOf(100))
                    .status("PENDING")
                    .build();

            when(campaignService.getPendingCampaigns()).thenReturn(List.of(campaign));

            mockMvc.perform(get("/api/admin/campaigns/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Pending Campaign"))
                    .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        @WithMockUser(username = "root@example.com", roles = {"ROOT_ADMIN"})
        @DisplayName("Should return pending campaigns for ROOT_ADMIN")
        void getPendingCampaigns_RootAdmin() throws Exception {
            when(campaignService.getPendingCampaigns()).thenReturn(List.of());

            mockMvc.perform(get("/api/admin/campaigns/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        @DisplayName("Should approve campaign and return 200 OK")
        void approveCampaign_Success() throws Exception {
            UUID campaignId = UUID.randomUUID();
            doNothing().when(campaignService).approveCampaign(campaignId);

            mockMvc.perform(post("/api/admin/campaigns/{id}/approve", campaignId))
                    .andExpect(status().isOk());

            verify(campaignService).approveCampaign(campaignId);
        }

        @Test
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        @DisplayName("Should return 404 when approving non-existent campaign")
        void approveCampaign_NotFound() throws Exception {
            UUID campaignId = UUID.randomUUID();
            doThrow(new CampaignNotFoundException(campaignId.toString()))
                    .when(campaignService).approveCampaign(campaignId);

            mockMvc.perform(post("/api/admin/campaigns/{id}/approve", campaignId))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string("the campaign with the id " + campaignId + " was not found"));
        }

        @Test
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        @DisplayName("Should reject campaign and return 200 OK")
        void rejectCampaign_Success() throws Exception {
            UUID campaignId = UUID.randomUUID();
            doNothing().when(campaignService).rejectCampaign(campaignId);

            mockMvc.perform(post("/api/admin/campaigns/{id}/reject", campaignId))
                    .andExpect(status().isOk());

            verify(campaignService).rejectCampaign(campaignId);
        }

        @Test
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        @DisplayName("Should return ended-by-creator campaigns")
        void getEndedByCreatorCampaigns_Success() throws Exception {
            CampaignResponse campaign = CampaignResponse.builder()
                    .id(UUID.randomUUID())
                    .title("Ended Campaign")
                    .status("ENDED_BY_CREATOR")
                    .build();

            when(campaignService.getEndedByCreatorCampaigns()).thenReturn(List.of(campaign));

            mockMvc.perform(get("/api/admin/campaigns/ended-by-creator"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Ended Campaign"));
        }

        @Test
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        @DisplayName("Should approve campaign end and return 200 OK")
        void approveCampaignEnd_Success() throws Exception {
            UUID campaignId = UUID.randomUUID();
            doNothing().when(campaignService).approveCampaignEnd(campaignId);

            mockMvc.perform(post("/api/admin/campaigns/{id}/approve-end", campaignId))
                    .andExpect(status().isOk());

            verify(campaignService).approveCampaignEnd(campaignId);
        }
    }
}
