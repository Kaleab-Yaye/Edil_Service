package com.edil.controller;

import com.edil.config.SecurityConfig;
import com.edil.dto.response.CampaignDetailResponse;
import com.edil.dto.response.CampaignResponse;
import com.edil.dto.response.GetCampaignPaymentInfoResponse;
import com.edil.dto.response.GetNumberOfJoinedUsersResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicCampaignController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class PublicCampaignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CampaignService campaignService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AccountRepository accountRepository;

    @Nested
    @DisplayName("GET /api/campaigns/ongoing")
    class OngoingCampaignsTests {

        @Test
        @DisplayName("Should return 200 OK with paginated ongoing campaigns")
        void getOngoingCampaigns_Success() throws Exception {
            CampaignResponse campaign = CampaignResponse.builder()
                    .id(UUID.randomUUID())
                    .title("Ongoing Campaign")
                    .ticketPrice(BigDecimal.valueOf(100))
                    .targetEntries(50)
                    .joinedUsers(10)
                    .status("APPROVED")
                    .creatorName("Creator 1")
                    .build();

            Page<CampaignResponse> page = new PageImpl<>(List.of(campaign));
            when(campaignService.getPublicCampaigns(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/campaigns/ongoing")
                            .param("page", "0")
                            .param("size", "10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("Ongoing Campaign"))
                    .andExpect(jsonPath("$.content[0].targetEntries").value(50));
        }
    }

    @Nested
    @DisplayName("GET /api/campaigns/ended")
    class EndedCampaignsTests {

        @Test
        @DisplayName("Should return 200 OK with paginated ended campaigns")
        void getEndedCampaigns_Success() throws Exception {
            CampaignResponse campaign = CampaignResponse.builder()
                    .id(UUID.randomUUID())
                    .title("Ended Campaign")
                    .ticketPrice(BigDecimal.valueOf(50))
                    .targetEntries(20)
                    .joinedUsers(20)
                    .status("ENDED")
                    .creatorName("Creator 2")
                    .build();

            Page<CampaignResponse> page = new PageImpl<>(List.of(campaign));
            when(campaignService.getPublicCampaignsEnded(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/campaigns/ended")
                            .param("page", "0")
                            .param("size", "10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("Ended Campaign"))
                    .andExpect(jsonPath("$.content[0].status").value("ENDED"));
        }
    }

    @Nested
    @DisplayName("GET /api/campaigns/{id}")
    class CampaignDetailTests {

        @Test
        @DisplayName("Should return 200 OK with campaign detail")
        void getCampaignDetail_Success() throws Exception {
            UUID campaignId = UUID.randomUUID();
            CampaignDetailResponse detail = CampaignDetailResponse.builder()
                    .id(campaignId)
                    .title("Special Campaign")
                    .aboutCampaign("About description")
                    .ticketPrice(BigDecimal.valueOf(250))
                    .targetEntries(100)
                    .joinedUsers(45)
                    .availableSlots(55)
                    .status("APPROVED")
                    .creatorName("Famous Creator")
                    .build();

            when(campaignService.getCampaignDetail(campaignId)).thenReturn(detail);

            mockMvc.perform(get("/api/campaigns/{id}", campaignId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(campaignId.toString()))
                    .andExpect(jsonPath("$.title").value("Special Campaign"))
                    .andExpect(jsonPath("$.availableSlots").value(55));
        }

        @Test
        @DisplayName("Should return 404 Not Found when campaign does not exist")
        void getCampaignDetail_NotFound() throws Exception {
            UUID campaignId = UUID.randomUUID();
            when(campaignService.getCampaignDetail(campaignId))
                    .thenThrow(new CampaignNotFoundException(campaignId.toString()));

            mockMvc.perform(get("/api/campaigns/{id}", campaignId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string("the campaign with the id " + campaignId + " was not found"));
        }
    }

    @Nested
    @DisplayName("GET /api/campaigns/payment/detail")
    class CampaignPaymentDetailTests {

        @Test
        @DisplayName("Should return 200 OK with campaign payment information")
        void getCampaignPaymentDetail_Success() throws Exception {
            UUID campaignId = UUID.randomUUID();
            GetCampaignPaymentInfoResponse response = new GetCampaignPaymentInfoResponse(
                    BigDecimal.valueOf(150),
                    "1000987654321",
                    "Abebe Kebede"
            );

            when(campaignService.getCampaignPaymentInfo(campaignId))
                    .thenReturn(ResponseEntity.ok(response));

            mockMvc.perform(get("/api/campaigns/payment/detail")
                            .param("campaignId", campaignId.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ticketPrice").value(150))
                    .andExpect(jsonPath("$.accountNumber").value("1000987654321"))
                    .andExpect(jsonPath("$.accountHolderName").value("Abebe Kebede"));
        }
    }

    @Nested
    @DisplayName("GET /api/campaigns/joined/users/count")
    class JoinedUsersCountSecurityTests {

        @Test
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        @DisplayName("Should allow ADMIN role and return count")
        void getJoinedUsersCount_AdminAllowed() throws Exception {
            UUID campaignId = UUID.randomUUID();
            GetNumberOfJoinedUsersResponse response = new GetNumberOfJoinedUsersResponse(42);

            when(campaignService.getNumberOfJoinedUsers(eq(campaignId), any()))
                    .thenReturn(ResponseEntity.ok(response));

            mockMvc.perform(get("/api/campaigns/joined/users/count")
                            .param("id", campaignId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.joinedUsers").value(42));
        }

        @Test
        @WithMockUser(username = "creator@example.com", roles = {"CREATOR"})
        @DisplayName("Should allow CREATOR role and return count")
        void getJoinedUsersCount_CreatorAllowed() throws Exception {
            UUID campaignId = UUID.randomUUID();
            GetNumberOfJoinedUsersResponse response = new GetNumberOfJoinedUsersResponse(15);

            when(campaignService.getNumberOfJoinedUsers(eq(campaignId), any()))
                    .thenReturn(ResponseEntity.ok(response));

            mockMvc.perform(get("/api/campaigns/joined/users/count")
                            .param("id", campaignId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.joinedUsers").value(15));
        }

        @Test
        @WithMockUser(username = "root@example.com", roles = {"ROOT_ADMIN"})
        @DisplayName("Should allow ROOT_ADMIN role and return count")
        void getJoinedUsersCount_RootAdminAllowed() throws Exception {
            UUID campaignId = UUID.randomUUID();
            GetNumberOfJoinedUsersResponse response = new GetNumberOfJoinedUsersResponse(99);

            when(campaignService.getNumberOfJoinedUsers(eq(campaignId), any()))
                    .thenReturn(ResponseEntity.ok(response));

            mockMvc.perform(get("/api/campaigns/joined/users/count")
                            .param("id", campaignId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.joinedUsers").value(99));
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Should reject USER role with 401 Unauthorized (GlobalExceptionHandler mapping)")
        void getJoinedUsersCount_UserDenied() throws Exception {
            UUID campaignId = UUID.randomUUID();

            mockMvc.perform(get("/api/campaigns/joined/users/count")
                            .param("id", campaignId.toString()))
                    .andExpect(status().isUnauthorized());
        }
    }
}
