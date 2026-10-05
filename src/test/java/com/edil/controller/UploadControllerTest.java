package com.edil.controller;

import com.edil.config.SecurityConfig;
import com.edil.domain.Account;
import com.edil.domain.CreatorProfile;
import com.edil.domain.enums.OnboardingStatus;
import com.edil.dto.response.PresignResponse;
import com.edil.exception.GlobalExceptionHandler;
import com.edil.repository.AccountRepository;
import com.edil.security.JwtAuthenticationFilter;
import com.edil.security.JwtService;
import com.edil.service.UploadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UploadController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class UploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UploadService uploadService;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private JwtService jwtService;

    @Nested
    @DisplayName("POST /api/uploads/presign")
    class PresignTests {

        @Test
        @DisplayName("Should reject unauthenticated requests with 403 Forbidden")
        void presign_Unauthenticated_Denied() throws Exception {
            mockMvc.perform(post("/api/uploads/presign"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Should reject USER role with 401 Unauthorized")
        void presign_UserRole_Denied() throws Exception {
            mockMvc.perform(post("/api/uploads/presign"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "creator@example.com", roles = {"CREATOR"})
        @DisplayName("Should return 403 Forbidden when creator is not ONBOARDED")
        void presign_PendingCreator_Forbidden() throws Exception {
            Account account = Account.builder()
                    .id(UUID.randomUUID())
                    .email("creator@example.com")
                    .creatorProfile(CreatorProfile.builder().onboardingStatus(OnboardingStatus.PENDING).build())
                    .build();

            when(accountRepository.findByEmail("creator@example.com")).thenReturn(Optional.of(account));

            mockMvc.perform(post("/api/uploads/presign"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "creator@example.com", roles = {"CREATOR"})
        @DisplayName("Should return 200 OK with PresignResponse when creator is ONBOARDED")
        void presign_OnboardedCreator_Success() throws Exception {
            UUID accountId = UUID.randomUUID();
            Account account = Account.builder()
                    .id(accountId)
                    .email("creator@example.com")
                    .creatorProfile(CreatorProfile.builder().onboardingStatus(OnboardingStatus.ONBOARDED).build())
                    .build();

            PresignResponse response = PresignResponse.builder()
                    .fileId("file-abc-123")
                    .uploadUrl("/uploads/file-abc-123.jpg")
                    .build();

            when(accountRepository.findByEmail("creator@example.com")).thenReturn(Optional.of(account));
            when(uploadService.generatePresignedTicket(accountId, ".jpg")).thenReturn(response);

            mockMvc.perform(post("/api/uploads/presign")
                            .param("extension", ".jpg"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fileId").value("file-abc-123"))
                    .andExpect(jsonPath("$.uploadUrl").value("/uploads/file-abc-123.jpg"));
        }
    }

    @Nested
    @DisplayName("GET /api/uploads/auth-check")
    class AuthCheckTests {

        @Test
        @DisplayName("Should return 401 Unauthorized when unauthenticated")
        void authCheck_Unauthenticated() throws Exception {
            mockMvc.perform(get("/api/uploads/auth-check")
                            .param("fileId", "file-123"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "creator@example.com")
        @DisplayName("Should return 200 OK when ticket is valid for user")
        void authCheck_ValidTicket() throws Exception {
            UUID accountId = UUID.randomUUID();
            Account account = Account.builder().id(accountId).email("creator@example.com").build();

            when(accountRepository.findByEmail("creator@example.com")).thenReturn(Optional.of(account));
            when(uploadService.validateTicketForNginxAuth("file-123", accountId)).thenReturn(true);

            mockMvc.perform(get("/api/uploads/auth-check")
                            .param("fileId", "file-123"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "creator@example.com")
        @DisplayName("Should return 401 Unauthorized when ticket is invalid")
        void authCheck_InvalidTicket() throws Exception {
            UUID accountId = UUID.randomUUID();
            Account account = Account.builder().id(accountId).email("creator@example.com").build();

            when(accountRepository.findByEmail("creator@example.com")).thenReturn(Optional.of(account));
            when(uploadService.validateTicketForNginxAuth("file-123", accountId)).thenReturn(false);

            mockMvc.perform(get("/api/uploads/auth-check")
                            .param("fileId", "file-123"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/uploads/confirm")
    class ConfirmUploadTests {

        @Test
        @WithMockUser(username = "creator@example.com")
        @DisplayName("Should confirm upload and return 200 OK")
        void confirmUpload_Success() throws Exception {
            UUID accountId = UUID.randomUUID();
            Account account = Account.builder().id(accountId).email("creator@example.com").build();

            when(accountRepository.findByEmail("creator@example.com")).thenReturn(Optional.of(account));
            doNothing().when(uploadService).confirmUpload("file-123", accountId);

            mockMvc.perform(post("/api/uploads/confirm")
                            .param("fileId", "file-123"))
                    .andExpect(status().isOk());

            verify(uploadService).confirmUpload("file-123", accountId);
        }
    }
}
