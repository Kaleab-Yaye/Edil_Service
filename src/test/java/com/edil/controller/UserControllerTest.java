package com.edil.controller;

import com.edil.config.SecurityConfig;
import com.edil.dto.request.GetUserProfileRequest;
import com.edil.dto.response.UserMeResponse;
import com.edil.exception.GlobalExceptionHandler;
import com.edil.repository.AccountRepository;
import com.edil.security.JwtAuthenticationFilter;
import com.edil.security.JwtService;
import com.edil.service.UserService;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private AccountRepository accountRepository;

    @Nested
    @DisplayName("GET /api/users/me")
    class GetMeTests {

        @Test
        @DisplayName("Should reject unauthenticated request with 403 Forbidden")
        void getMe_Unauthenticated_Denied() throws Exception {
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("Should return 200 OK with UserMeResponse for authenticated user")
        void getMe_Authenticated_Success() throws Exception {
            UserMeResponse response = UserMeResponse.builder()
                    .accountId(UUID.randomUUID())
                    .email("user@example.com")
                    .fullName("User FullName")
                    .role("USER")
                    .active(true)
                    .build();

            when(userService.getMe("user@example.com")).thenReturn(response);

            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("user@example.com"))
                    .andExpect(jsonPath("$.role").value("USER"));
        }
    }

    @Nested
    @DisplayName("POST /api/users/user")
    class GetUserProfileByAdminTests {

        @Test
        @DisplayName("Should reject unauthenticated request with 403 Forbidden")
        void getUserProfile_Unauthenticated_Denied() throws Exception {
            GetUserProfileRequest request = new GetUserProfileRequest(UUID.randomUUID());

            mockMvc.perform(post("/api/users/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Should reject USER role with 401 Unauthorized")
        void getUserProfile_UserRole_Denied() throws Exception {
            GetUserProfileRequest request = new GetUserProfileRequest(UUID.randomUUID());

            mockMvc.perform(post("/api/users/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        @DisplayName("Should allow ADMIN role and return UserMeResponse")
        void getUserProfile_AdminRole_Success() throws Exception {
            UUID targetUserId = UUID.randomUUID();
            GetUserProfileRequest request = new GetUserProfileRequest(targetUserId);
            UserMeResponse response = UserMeResponse.builder()
                    .accountId(targetUserId)
                    .email("target@example.com")
                    .fullName("Target User")
                    .role("USER")
                    .active(true)
                    .build();

            when(userService.getUserDetails(any(GetUserProfileRequest.class)))
                    .thenReturn(ResponseEntity.ok(response));

            mockMvc.perform(post("/api/users/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("target@example.com"))
                    .andExpect(jsonPath("$.fullName").value("Target User"));
        }
    }
}
