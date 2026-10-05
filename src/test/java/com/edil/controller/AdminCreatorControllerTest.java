package com.edil.controller;

import com.edil.config.SecurityConfig;
import com.edil.dto.response.CreatorQueueItemResponse;
import com.edil.exception.AccountNotFoundException;
import com.edil.exception.GlobalExceptionHandler;
import com.edil.repository.AccountRepository;
import com.edil.security.JwtAuthenticationFilter;
import com.edil.security.JwtService;
import com.edil.service.CreatorOnboardingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminCreatorController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class AdminCreatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreatorOnboardingService onboardingService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private AccountRepository accountRepository;

    @Nested
    @DisplayName("Security Authorization Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should reject unauthenticated request with 403 Forbidden")
        void unauthenticated_Denied() throws Exception {
            mockMvc.perform(get("/api/admin/creators/queue"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Should reject USER role with 403 Forbidden")
        void userRole_Denied() throws Exception {
            mockMvc.perform(get("/api/admin/creators/queue"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "creator@example.com", roles = {"CREATOR"})
        @DisplayName("Should reject CREATOR role with 403 Forbidden")
        void creatorRole_Denied() throws Exception {
            mockMvc.perform(get("/api/admin/creators/queue"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Admin Creator Queue & Actions Tests")
    class ExecutionTests {

        @Test
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        @DisplayName("Should return creator onboarding queue for ADMIN")
        void getQueue_Success() throws Exception {
            UUID accountId = UUID.randomUUID();
            CreatorQueueItemResponse item = CreatorQueueItemResponse.builder()
                    .accountId(accountId)
                    .email("applicant@example.com")
                    .fullName("Applicant Name")
                    .onboardingStatus("PENDING")
                    .build();

            when(onboardingService.getQueue()).thenReturn(List.of(item));

            mockMvc.perform(get("/api/admin/creators/queue"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].email").value("applicant@example.com"))
                    .andExpect(jsonPath("$[0].fullName").value("Applicant Name"))
                    .andExpect(jsonPath("$[0].onboardingStatus").value("PENDING"));
        }

        @Test
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        @DisplayName("Should approve creator onboarding and return 200 OK")
        void approveCreator_Success() throws Exception {
            UUID accountId = UUID.randomUUID();
            doNothing().when(onboardingService).approveCreator(accountId);

            mockMvc.perform(post("/api/admin/creators/{accountId}/approve", accountId))
                    .andExpect(status().isOk());

            verify(onboardingService).approveCreator(accountId);
        }

        @Test
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        @DisplayName("Should return 404 when approving non-existent creator")
        void approveCreator_NotFound() throws Exception {
            UUID accountId = UUID.randomUUID();
            doThrow(new AccountNotFoundException("Account not found"))
                    .when(onboardingService).approveCreator(accountId);

            mockMvc.perform(post("/api/admin/creators/{accountId}/approve", accountId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_FOUND"));
        }

        @Test
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        @DisplayName("Should reject creator onboarding and return 200 OK")
        void rejectCreator_Success() throws Exception {
            UUID accountId = UUID.randomUUID();
            doNothing().when(onboardingService).rejectCreator(accountId);

            mockMvc.perform(post("/api/admin/creators/{accountId}/reject", accountId))
                    .andExpect(status().isOk());

            verify(onboardingService).rejectCreator(accountId);
        }
    }
}
