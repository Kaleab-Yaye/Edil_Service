package com.edil.controller;

import com.edil.config.SecurityConfig;
import com.edil.exception.GlobalExceptionHandler;
import com.edil.repository.AccountRepository;
import com.edil.security.JwtAuthenticationFilter;
import com.edil.security.JwtService;
import com.edil.service.CreatorsCampaignService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DownloadController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class DownloadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreatorsCampaignService creatorsCampaignService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AccountRepository accountRepository;

    @Test
    @DisplayName("Should return 200 OK when download key and URI are valid (permitAll)")
    void downloadPdfAuthController_Authorized() throws Exception {
        UUID key = UUID.randomUUID();
        String uri = "/pdf_store/test_campaign.pdf";

        when(creatorsCampaignService.canDownloadPdf(key, uri)).thenReturn(true);

        mockMvc.perform(get("/api/v1/download/report/internal/auth")
                        .param("key", key.toString())
                        .header("X-Original-URI", uri))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when download key is invalid or expired")
    void downloadPdfAuthController_Forbidden() throws Exception {
        UUID key = UUID.randomUUID();
        String uri = "/pdf_store/unauthorized.pdf";

        when(creatorsCampaignService.canDownloadPdf(key, uri)).thenReturn(false);

        mockMvc.perform(get("/api/v1/download/report/internal/auth")
                        .param("key", key.toString())
                        .header("X-Original-URI", uri))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when key query parameter is missing")
    void downloadPdfAuthController_MissingKey() throws Exception {
        mockMvc.perform(get("/api/v1/download/report/internal/auth")
                        .header("X-Original-URI", "/pdf_store/file.pdf"))
                .andExpect(status().isBadRequest());
    }
}
