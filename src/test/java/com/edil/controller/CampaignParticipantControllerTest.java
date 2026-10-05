package com.edil.controller;

import com.edil.config.SecurityConfig;
import com.edil.domain.enums.PetitionReason;
import com.edil.dto.request.*;
import com.edil.dto.response.*;
import com.edil.exception.GlobalExceptionHandler;
import com.edil.repository.AccountRepository;
import com.edil.security.JwtAuthenticationFilter;
import com.edil.security.JwtService;
import com.edil.service.CampaignParticipantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CampaignParticipantController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class CampaignParticipantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CampaignParticipantService campaignParticipantService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private AccountRepository accountRepository;

    @Nested
    @DisplayName("Participant Authentication Enforcement Tests")
    class AuthenticationEnforcementTests {

        @Test
        @DisplayName("POST /can/join - Should reject unauthenticated requests")
        void canJoin_Unauthenticated_Denied() throws Exception {
            CanParticipantJoinCampaignRequest request = new CanParticipantJoinCampaignRequest(UUID.randomUUID());

            mockMvc.perform(post("/api/v1/participant/can/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /ongoing/payment - Should reject unauthenticated requests")
        void ongoingPayment_Unauthenticated_Denied() throws Exception {
            mockMvc.perform(get("/api/v1/participant/ongoing/payment"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /reserve/slot - Should reject unauthenticated requests")
        void reserveSlot_Unauthenticated_Denied() throws Exception {
            CreateCampaignSlotForUserRequest request = new CreateCampaignSlotForUserRequest(UUID.randomUUID());

            mockMvc.perform(post("/api/v1/participant/reserve/slot")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /add/participant - Should reject unauthenticated requests")
        void addParticipant_Unauthenticated_Denied() throws Exception {
            AddParticipantToCampaignRequest request = new AddParticipantToCampaignRequest(UUID.randomUUID(), UUID.randomUUID(), "link");

            mockMvc.perform(post("/api/v1/participant/add/participant")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /cancel/slot - Should reject unauthenticated requests")
        void cancelSlot_Unauthenticated_Denied() throws Exception {
            mockMvc.perform(get("/api/v1/participant/cancel/slot"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /submit/petition - Should reject unauthenticated requests")
        void submitPetition_Unauthenticated_Denied() throws Exception {
            CreatePetitionRequest request = new CreatePetitionRequest("link", PetitionReason.PAYMENT_REFUSED, "statement", UUID.randomUUID());

            mockMvc.perform(post("/api/v1/participant/submit/petition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Authenticated Participant Operations")
    class AuthenticatedOperationsTests {

        @Test
        @WithMockUser(username = "participant@example.com")
        @DisplayName("POST /can/join - Should return can join status")
        void canJoin_Success() throws Exception {
            UUID campaignId = UUID.randomUUID();
            UUID slotKey = UUID.randomUUID();
            CanParticipantJoinCampaignRequest request = new CanParticipantJoinCampaignRequest(campaignId);
            CanParticipantJoinCampaignResponse response = new CanParticipantJoinCampaignResponse(
                    true,
                    false,
                    campaignId,
                    slotKey,
                    false
            );

            when(campaignParticipantService.canParticipantJoinCampaign(any(CanParticipantJoinCampaignRequest.class), any()))
                    .thenReturn(ResponseEntity.ok(response));

            mockMvc.perform(post("/api/v1/participant/can/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.canJoin").value(true))
                    .andExpect(jsonPath("$.haOnGoingPetition").value(false));
        }

        @Test
        @WithMockUser(username = "participant@example.com")
        @DisplayName("GET /ongoing/payment - Should return reserved slot details")
        void ongoingPayment_Success() throws Exception {
            UUID slotKey = UUID.randomUUID();
            UUID campaignId = UUID.randomUUID();
            FetchOnGoingSlotInformationForUserResponse response = new FetchOnGoingSlotInformationForUserResponse(
                    true,
                    300L,
                    slotKey,
                    campaignId,
                    "100012345678",
                    BigDecimal.valueOf(100),
                    "Campaign Creator"
            );

            when(campaignParticipantService.fetchOngoingUserSlotInfo(any()))
                    .thenReturn(ResponseEntity.ok(response));

            mockMvc.perform(get("/api/v1/participant/ongoing/payment"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasReservedSlot").value(true))
                    .andExpect(jsonPath("$.leftTimeInSeconds").value(300))
                    .andExpect(jsonPath("$.accountNumber").value("100012345678"));
        }

        @Test
        @WithMockUser(username = "participant@example.com")
        @DisplayName("POST /reserve/slot - Should reserve slot for user")
        void reserveSlot_Success() throws Exception {
            UUID campaignId = UUID.randomUUID();
            UUID slotKey = UUID.randomUUID();
            CreateCampaignSlotForUserRequest request = new CreateCampaignSlotForUserRequest(campaignId);
            CreateCampaignSlotForUserResponse response = new CreateCampaignSlotForUserResponse(
                    slotKey,
                    true,
                    "100012345678",
                    BigDecimal.valueOf(250),
                    "Account Owner"
            );

            when(campaignParticipantService.createCampaignSlotForUser(any(CreateCampaignSlotForUserRequest.class), any()))
                    .thenReturn(ResponseEntity.ok(response));

            mockMvc.perform(post("/api/v1/participant/reserve/slot")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slotAvailable").value(true))
                    .andExpect(jsonPath("$.accountNumber").value("100012345678"));
        }

        @Test
        @WithMockUser(username = "participant@example.com")
        @DisplayName("POST /add/participant - Should add participant")
        void addParticipant_Success() throws Exception {
            UUID campaignId = UUID.randomUUID();
            UUID slotKey = UUID.randomUUID();
            AddParticipantToCampaignRequest request = new AddParticipantToCampaignRequest(campaignId, slotKey, "https://mbreciept.cbe.com.et/v2-ABC");
            AddParticipantToCampaignResponse response = new AddParticipantToCampaignResponse("Successfully added to campaign");

            when(campaignParticipantService.AddCampaignParticipant(any(AddParticipantToCampaignRequest.class), any()))
                    .thenReturn(ResponseEntity.ok(response));

            mockMvc.perform(post("/api/v1/participant/add/participant")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Successfully added to campaign"));
        }

        @Test
        @WithMockUser(username = "participant@example.com")
        @DisplayName("GET /cancel/slot - Should cancel reserved slot")
        void cancelSlot_Success() throws Exception {
            when(campaignParticipantService.cancelReservedSlot(any()))
                    .thenReturn(ResponseEntity.ok(HttpStatus.OK));

            mockMvc.perform(get("/api/v1/participant/cancel/slot"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "participant@example.com")
        @DisplayName("POST /submit/petition - Should submit petition")
        void submitPetition_Success() throws Exception {
            UUID campaignId = UUID.randomUUID();
            CreatePetitionRequest request = new CreatePetitionRequest(
                    "https://cbe.et/123",
                    PetitionReason.REFUND_REQUEST,
                    "Payment was sent but not counted",
                    campaignId
            );
            CreatePetitionResponse response = new CreatePetitionResponse("Petition submitted successfully");

            when(campaignParticipantService.submitPetition(any(CreatePetitionRequest.class), any()))
                    .thenReturn(ResponseEntity.ok(response));

            mockMvc.perform(post("/api/v1/participant/submit/petition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Petition submitted successfully"));
        }
    }

    @Nested
    @DisplayName("Admin Add Participant Security & Operations")
    class AdminAddParticipantTests {

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("POST admin/add/participant - Should reject USER role with 401 Unauthorized")
        void addParticipantByAdmin_UserRole_Denied() throws Exception {
            AddUserTOCampaignByAdminRequest request = new AddUserTOCampaignByAdminRequest(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "https://cbe.et/123"
            );

            mockMvc.perform(post("/api/v1/participant/admin/add/participant")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        @DisplayName("POST admin/add/participant - Should allow ADMIN role")
        void addParticipantByAdmin_AdminRole_Success() throws Exception {
            AddUserTOCampaignByAdminRequest request = new AddUserTOCampaignByAdminRequest(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "https://cbe.et/123"
            );
            AddUserTOCampaignByAdminResponse response = new AddUserTOCampaignByAdminResponse(true, "Added successfully");

            when(campaignParticipantService.addUserTOCampaignByAdmin(any(AddUserTOCampaignByAdminRequest.class), any()))
                    .thenReturn(ResponseEntity.ok(response));

            mockMvc.perform(post("/api/v1/participant/admin/add/participant")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.added").value(true))
                    .andExpect(jsonPath("$.message").value("Added successfully"));
        }
    }

    @Nested
    @DisplayName("Public Add Participant Endpoints")
    class PublicAddParticipantTests {

        @Test
        @DisplayName("POST /public/add/participant?format=link - Should succeed with valid link payload")
        void publicAdd_LinkFormat_Success() throws Exception {
            UUID campaignId = UUID.randomUUID();
            AddParticipantToCampaignFromOpenWithLinkRequest request = new AddParticipantToCampaignFromOpenWithLinkRequest(
                    "Abebe",
                    "Kebede",
                    "+251911223344",
                    "100012345678",
                    campaignId,
                    "https://mbreciept.cbe.com.et/v2-ABCDEF123456"
            );

            AddParticipantFromOpenResponse response = new AddParticipantFromOpenResponse();
            response.setMessage("Participant joined successfully");
            response.setEdilNumber("EDIL-001");

            when(campaignParticipantService.addCampaignParticipantFromPublic(any(AddParticipantToCampaignFromOpenWithLinkRequest.class)))
                    .thenReturn(ResponseEntity.ok(response));

            mockMvc.perform(post("/api/v1/participant/public/add/participant")
                            .param("format", "link")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Participant joined successfully"))
                    .andExpect(jsonPath("$.edilNumber").value("EDIL-001"));
        }

        @Test
        @DisplayName("POST /public/add/participant?format=link - Should return 400 when cbe link format is invalid")
        void publicAdd_LinkFormat_InvalidLinkPattern() throws Exception {
            UUID campaignId = UUID.randomUUID();
            AddParticipantToCampaignFromOpenWithLinkRequest request = new AddParticipantToCampaignFromOpenWithLinkRequest(
                    "Abebe",
                    "Kebede",
                    "+251911223344",
                    "100012345678",
                    campaignId,
                    "https://invalid-cbe-link.com"
            );

            mockMvc.perform(post("/api/v1/participant/public/add/participant")
                            .param("format", "link")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /public/add/participant?format=image - Should succeed with valid image payload")
        void publicAdd_ImageFormat_Success() throws Exception {
            UUID campaignId = UUID.randomUUID();
            UUID receiptKey = UUID.randomUUID();
            AddParticipantToCampaignFromOpenWithImageRequest request = new AddParticipantToCampaignFromOpenWithImageRequest(
                    "Abebe",
                    "Kebede",
                    "+251911223344",
                    "100012345678",
                    campaignId,
                    receiptKey
            );

            AddParticipantFromOpenResponse response = new AddParticipantFromOpenResponse();
            response.setMessage("Receipt image accepted");
            response.setEdilNumber("EDIL-002");

            when(campaignParticipantService.addCampaignParticipantFromPublicWithReceiptImage(any(AddParticipantToCampaignFromOpenWithImageRequest.class)))
                    .thenReturn(ResponseEntity.ok(response));

            mockMvc.perform(post("/api/v1/participant/public/add/participant")
                            .param("format", "image")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Receipt image accepted"))
                    .andExpect(jsonPath("$.edilNumber").value("EDIL-002"));
        }

        @Test
        @DisplayName("POST /public/add/participant?format=unknown - Should return 400 Bad Request")
        void publicAdd_UnknownFormat_BadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/participant/public/add/participant")
                            .param("format", "unknown")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
