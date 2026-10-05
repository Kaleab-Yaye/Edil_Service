package com.edil.controller;

import com.edil.config.SecurityConfig;
import com.edil.dto.request.CreateAdminRequest;
import com.edil.dto.response.AdminUserResponse;
import com.edil.exception.AccountNotFoundException;
import com.edil.exception.EmailAlreadyExistsException;
import com.edil.exception.GlobalExceptionHandler;
import com.edil.repository.AccountRepository;
import com.edil.security.JwtAuthenticationFilter;
import com.edil.security.JwtService;
import com.edil.service.AdminManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminManagementController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class AdminManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminManagementService adminManagementService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AccountRepository accountRepository;

    @Nested
    @DisplayName("Root Admin Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should reject unauthenticated request with 403 Forbidden")
        void unauthenticated_Denied() throws Exception {
            mockMvc.perform(get("/api/admin/management/admins"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Should reject USER role with 403 Forbidden")
        void userRole_Denied() throws Exception {
            mockMvc.perform(get("/api/admin/management/admins"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        @DisplayName("Should reject standard ADMIN role (requires ROOT_ADMIN) with 401 Unauthorized")
        void adminRole_DeniedRootAccess() throws Exception {
            mockMvc.perform(get("/api/admin/management/admins"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Root Admin Operations Tests")
    class OperationsTests {

        @Test
        @WithMockUser(username = "root@example.com", roles = {"ROOT_ADMIN"})
        @DisplayName("Should return all admins for ROOT_ADMIN")
        void getAllAdmins_Success() throws Exception {
            AdminUserResponse admin = AdminUserResponse.builder()
                    .accountId(UUID.randomUUID())
                    .email("admin@example.com")
                    .fullName("Admin Name")
                    .role("ADMIN")
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(adminManagementService.getAllAdmins()).thenReturn(List.of(admin));

            mockMvc.perform(get("/api/admin/management/admins"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].email").value("admin@example.com"))
                    .andExpect(jsonPath("$[0].role").value("ADMIN"));
        }

        @Test
        @WithMockUser(username = "root@example.com", roles = {"ROOT_ADMIN"})
        @DisplayName("Should successfully create admin with 201 Created")
        void createAdmin_Success() throws Exception {
            CreateAdminRequest request = CreateAdminRequest.builder()
                    .email("newadmin@example.com")
                    .password("SecurePass123!")
                    .fullName("New Admin")
                    .build();

            AdminUserResponse response = AdminUserResponse.builder()
                    .accountId(UUID.randomUUID())
                    .email("newadmin@example.com")
                    .fullName("New Admin")
                    .role("ADMIN")
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(adminManagementService.createAdmin(any(CreateAdminRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/admin/management/admins")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("newadmin@example.com"))
                    .andExpect(jsonPath("$.role").value("ADMIN"));
        }

        @Test
        @WithMockUser(username = "root@example.com", roles = {"ROOT_ADMIN"})
        @DisplayName("Should return 400 Bad Request when create admin request is invalid")
        void createAdmin_ValidationFailure() throws Exception {
            CreateAdminRequest request = CreateAdminRequest.builder()
                    .email("not-an-email")
                    .password("123")
                    .fullName("")
                    .build();

            mockMvc.perform(post("/api/admin/management/admins")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "root@example.com", roles = {"ROOT_ADMIN"})
        @DisplayName("Should return 409 Conflict when creating admin with existing email")
        void createAdmin_EmailAlreadyExists() throws Exception {
            CreateAdminRequest request = CreateAdminRequest.builder()
                    .email("existing@example.com")
                    .password("SecurePass123!")
                    .fullName("Existing Admin")
                    .build();

            when(adminManagementService.createAdmin(any(CreateAdminRequest.class)))
                    .thenThrow(new EmailAlreadyExistsException("Email already in use"));

            mockMvc.perform(post("/api/admin/management/admins")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"));
        }

        @Test
        @WithMockUser(username = "root@example.com", roles = {"ROOT_ADMIN"})
        @DisplayName("Should toggle admin status and return 200 OK")
        void toggleStatus_Success() throws Exception {
            UUID accountId = UUID.randomUUID();
            doNothing().when(adminManagementService).toggleAdminStatus(accountId, false);

            mockMvc.perform(patch("/api/admin/management/admins/{accountId}/status", accountId)
                            .param("active", "false"))
                    .andExpect(status().isOk());

            verify(adminManagementService).toggleAdminStatus(accountId, false);
        }

        @Test
        @WithMockUser(username = "root@example.com", roles = {"ROOT_ADMIN"})
        @DisplayName("Should return 404 when toggling status of non-existent admin")
        void toggleStatus_NotFound() throws Exception {
            UUID accountId = UUID.randomUUID();
            doThrow(new AccountNotFoundException("Admin not found"))
                    .when(adminManagementService).toggleAdminStatus(accountId, true);

            mockMvc.perform(patch("/api/admin/management/admins/{accountId}/status", accountId)
                            .param("active", "true"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_FOUND"));
        }
    }
}
