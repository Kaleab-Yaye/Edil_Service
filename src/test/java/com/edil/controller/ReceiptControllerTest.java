package com.edil.controller;

import com.edil.config.SecurityConfig;
import com.edil.dto.request.HasReceiptBeenUsedBeforeRequest;
import com.edil.dto.response.HasReceiptBeenUsedBeforeResponse;
import com.edil.dto.response.UploadReceiptResponse;
import com.edil.exception.GlobalExceptionHandler;
import com.edil.repository.AccountRepository;
import com.edil.security.JwtAuthenticationFilter;
import com.edil.security.JwtService;
import com.edil.service.ReceiptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReceiptController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class ReceiptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReceiptService receiptService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AccountRepository accountRepository;

    @Nested
    @DisplayName("POST /api/v1/receipt/check/use - Security & Execution")
    class CheckUseTests {

        @Test
        @DisplayName("Should reject unauthenticated requests with 403 Forbidden")
        void checkUse_Unauthenticated_Denied() throws Exception {
            HasReceiptBeenUsedBeforeRequest request = new HasReceiptBeenUsedBeforeRequest("https://cbe.et/123");

            mockMvc.perform(post("/api/v1/receipt/check/use")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Should reject USER role with 401 Unauthorized")
        void checkUse_UserRole_Denied() throws Exception {
            HasReceiptBeenUsedBeforeRequest request = new HasReceiptBeenUsedBeforeRequest("https://cbe.et/123");

            mockMvc.perform(post("/api/v1/receipt/check/use")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        @DisplayName("Should allow ADMIN role and return usage check response")
        void checkUse_AdminRole_Success() throws Exception {
            HasReceiptBeenUsedBeforeRequest request = new HasReceiptBeenUsedBeforeRequest("https://cbe.et/123");
            HasReceiptBeenUsedBeforeResponse response = new HasReceiptBeenUsedBeforeResponse(false, true);

            when(receiptService.checkForReceiptExitance(any(HasReceiptBeenUsedBeforeRequest.class)))
                    .thenReturn(ResponseEntity.ok(response));

            mockMvc.perform(post("/api/v1/receipt/check/use")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasBeenUsed").value(false))
                    .andExpect(jsonPath("$.isValidUrl").value(true));
        }
    }

    @Nested
    @DisplayName("Public Receipt Endpoints Tests")
    class PublicEndpointsTests {

        @Test
        @DisplayName("GET /api/v1/receipt/create/upload/key - Should permit all and return upload key")
        void createUploadKey_Success() throws Exception {
            UUID uploadKey = UUID.randomUUID();
            UploadReceiptResponse response = new UploadReceiptResponse(uploadKey);

            when(receiptService.createUploadReceiptKey()).thenReturn(ResponseEntity.ok(response));

            mockMvc.perform(get("/api/v1/receipt/create/upload/key"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uploadKey").value(uploadKey.toString()));
        }

        @Test
        @DisplayName("GET /api/v1/receipt/check/upload/auth - Should permit all and check key from header")
        void checkUploadAuth_Success() throws Exception {
            String originalUri = "/upload/receipt?key=some-valid-key";
            when(receiptService.checkForUploadReceiptKey(originalUri))
                    .thenReturn(ResponseEntity.ok().build());

            mockMvc.perform(get("/api/v1/receipt/check/upload/auth")
                            .header("X-Original-URI", originalUri))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/v1/receipt/check/upload/auth - Should return 401 when key is invalid")
        void checkUploadAuth_Unauthorized() throws Exception {
            String originalUri = "/upload/receipt?key=invalid-key";
            when(receiptService.checkForUploadReceiptKey(originalUri))
                    .thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

            mockMvc.perform(get("/api/v1/receipt/check/upload/auth")
                            .header("X-Original-URI", originalUri))
                    .andExpect(status().isUnauthorized());
        }
    }
}
