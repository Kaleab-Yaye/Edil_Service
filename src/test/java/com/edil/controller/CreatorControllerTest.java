package com.edil.controller;

import com.edil.config.SecurityConfig;
import com.edil.exception.GlobalExceptionHandler;
import com.edil.repository.AccountRepository;
import com.edil.security.JwtAuthenticationFilter;
import com.edil.security.JwtService;
import com.edil.service.CreatorOnboardingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CreatorController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class CreatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreatorOnboardingService onboardingService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private AccountRepository accountRepository;

    @Test
    @DisplayName("Should reject unauthenticated reapply with 403 Forbidden")
    void reapply_Unauthenticated_Denied() throws Exception {
        mockMvc.perform(post("/api/creators/reapply"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "creator@example.com")
    @DisplayName("Should allow authenticated user to reapply and return 200 OK")
    void reapply_Success() throws Exception {
        doNothing().when(onboardingService).reapply("creator@example.com");

        mockMvc.perform(post("/api/creators/reapply"))
                .andExpect(status().isOk());

        verify(onboardingService).reapply("creator@example.com");
    }
}
