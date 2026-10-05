package com.edil.controller;

import com.edil.config.SecurityConfig;
import com.edil.domain.enums.PetitionReason;
import com.edil.domain.enums.PetitionStatus;
import com.edil.dto.request.HandlePetitionRequest;
import com.edil.dto.request.ResolvePetitionRequest;
import com.edil.dto.response.GetPetitionsBeingHandledByMeResponse;
import com.edil.dto.response.GetUnresolvedPetitionsResponse;
import com.edil.dto.response.HandlePetitionResponse;
import com.edil.dto.response.ResolvePetitionResponse;
import com.edil.exception.GlobalExceptionHandler;
import com.edil.repository.AccountRepository;
import com.edil.security.JwtAuthenticationFilter;
import com.edil.security.JwtService;
import com.edil.service.PetitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PetitionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class PetitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PetitionService petitionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AccountRepository accountRepository;

    @Nested
    @DisplayName("Petition Role Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should reject unauthenticated requests with 403 Forbidden")
        void unauthenticated_Denied() throws Exception {
            mockMvc.perform(get("/api/v1/petition/unresolved"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Should reject USER role with 401 Unauthorized")
        void userRole_Denied() throws Exception {
            mockMvc.perform(get("/api/v1/petition/unresolved"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "creator@example.com", roles = {"CREATOR"})
        @DisplayName("Should reject CREATOR role with 401 Unauthorized")
        void creatorRole_Denied() throws Exception {
            mockMvc.perform(get("/api/v1/petition/unresolved"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Petition Handling & Resolution Tests")
    class OperationsTests {

        @Test
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        @DisplayName("GET /api/v1/petition/unresolved - Should return unresolved petitions")
        void getUnresolvedPetitions_Success() throws Exception {
            GetUnresolvedPetitionsResponse item = GetUnresolvedPetitionsResponse.builder()
                    .id(UUID.randomUUID())
                    .petitionerId(UUID.randomUUID())
                    .campaignId(UUID.randomUUID())
                    .paymentLink("https://cbe.et/tx/12345")
                    .petitionReason(PetitionReason.REFUND_REQUEST)
                    .createdAt(LocalDateTime.now())
                    .build();

            Page<GetUnresolvedPetitionsResponse> page = new PageImpl<>(List.of(item));
            when(petitionService.getUnresolvedPetitions(any(Pageable.class))).thenReturn(ResponseEntity.ok(page));

            mockMvc.perform(get("/api/v1/petition/unresolved")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].paymentLink").value("https://cbe.et/tx/12345"))
                    .andExpect(jsonPath("$.content[0].petitionReason").value("REFUND_REQUEST"));
        }

        @Test
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        @DisplayName("POST /api/v1/petition/handle - Should take ownership of petition")
        void handlePetition_Success() throws Exception {
            UUID petitionId = UUID.randomUUID();
            HandlePetitionRequest request = new HandlePetitionRequest(petitionId);
            HandlePetitionResponse response = new HandlePetitionResponse(true);

            when(petitionService.handlePetition(any(HandlePetitionRequest.class), any()))
                    .thenReturn(ResponseEntity.ok(response));

            mockMvc.perform(post("/api/v1/petition/handle")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.becameHandler").value(true));
        }

        @Test
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        @DisplayName("POST /api/v1/petition/resolve - Should resolve petition successfully")
        void resolvePetition_Success() throws Exception {
            UUID petitionId = UUID.randomUUID();
            ResolvePetitionRequest request = new ResolvePetitionRequest(petitionId, PetitionStatus.APPROVED);
            ResolvePetitionResponse response = new ResolvePetitionResponse(true, "Petition resolved successfully");

            when(petitionService.resolvePetition(any(ResolvePetitionRequest.class), any()))
                    .thenReturn(ResponseEntity.ok(response));

            mockMvc.perform(post("/api/v1/petition/resolve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resolved").value(true))
                    .andExpect(jsonPath("$.message").value("Petition resolved successfully"));
        }

        @Test
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        @DisplayName("POST /api/v1/petition/resolve - Should return 400 Bad Request on invalid payload")
        void resolvePetition_ValidationFailure() throws Exception {
            ResolvePetitionRequest request = new ResolvePetitionRequest(null, null);

            mockMvc.perform(post("/api/v1/petition/resolve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        @DisplayName("GET /api/v1/petition/admin/being/handled - Should return petitions being handled by admin")
        void getPetitionsBeingHandledByMe_Admin() throws Exception {
            GetPetitionsBeingHandledByMeResponse item = GetPetitionsBeingHandledByMeResponse.builder()
                    .id(UUID.randomUUID())
                    .petitionerId(UUID.randomUUID())
                    .campaignId(UUID.randomUUID())
                    .paymentLink("https://cbe.et/tx/999")
                    .petitionReason(PetitionReason.PAYMENT_REFUSED)
                    .createdAt(LocalDateTime.now())
                    .build();

            Page<GetPetitionsBeingHandledByMeResponse> page = new PageImpl<>(List.of(item));
            when(petitionService.getBeingHandledPetitionsByAdmin(any(Pageable.class), any()))
                    .thenReturn(ResponseEntity.ok(page));

            mockMvc.perform(get("/api/v1/petition/admin/being/handled")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].paymentLink").value("https://cbe.et/tx/999"));
        }

        @Test
        @WithMockUser(username = "root@example.com", roles = {"ROOT_ADMIN"})
        @DisplayName("GET /api/v1/petition/admin/being/handled - Should allow ROOT_ADMIN")
        void getPetitionsBeingHandledByMe_RootAdmin() throws Exception {
            Page<GetPetitionsBeingHandledByMeResponse> page = new PageImpl<>(List.of());
            when(petitionService.getBeingHandledPetitionsByAdmin(any(Pageable.class), any()))
                    .thenReturn(ResponseEntity.ok(page));

            mockMvc.perform(get("/api/v1/petition/admin/being/handled"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }
}
