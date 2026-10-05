package com.edil.service;

import com.edil.domain.Account;
import com.edil.domain.AdminProfile;
import com.edil.domain.enums.AccountRole;
import com.edil.dto.request.CreateAdminRequest;
import com.edil.dto.response.AdminUserResponse;
import com.edil.exception.AccountNotFoundException;
import com.edil.exception.EmailAlreadyExistsException;
import com.edil.repository.AccountRepository;
import com.edil.repository.AdminProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminManagementServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AdminProfileRepository adminProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminManagementService adminManagementService;

    private CreateAdminRequest createAdminRequest;

    @BeforeEach
    void setUp() {
        createAdminRequest = new CreateAdminRequest();
        createAdminRequest.setEmail("newadmin@edil.com");
        createAdminRequest.setPassword("AdminP@ss123");
        createAdminRequest.setFullName("Kaleb Admin");
    }

    @Test
    @DisplayName("getAllAdmins should return mapped response with admin profiles")
    void getAllAdmins_ShouldReturnMappedResponses() {
        UUID adminId = UUID.randomUUID();
        Account adminAccount = Account.builder()
                .id(adminId)
                .email("admin@edil.com")
                .role(AccountRole.ADMIN)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        AdminProfile profile = AdminProfile.builder()
                .id(UUID.randomUUID())
                .account(adminAccount)
                .fullName("Super Admin")
                .build();

        when(accountRepository.findAllByRoleIn(List.of(AccountRole.ADMIN, AccountRole.ROOT_ADMIN)))
                .thenReturn(List.of(adminAccount));
        when(adminProfileRepository.findByAccountId(adminId)).thenReturn(Optional.of(profile));

        List<AdminUserResponse> result = adminManagementService.getAllAdmins();

        assertThat(result).hasSize(1);
        AdminUserResponse resp = result.get(0);
        assertThat(resp.getAccountId()).isEqualTo(adminId);
        assertThat(resp.getEmail()).isEqualTo("admin@edil.com");
        assertThat(resp.getFullName()).isEqualTo("Super Admin");
        assertThat(resp.getRole()).isEqualTo("ADMIN");
        assertThat(resp.isActive()).isTrue();
    }

    @Test
    @DisplayName("getAllAdmins should fallback to 'System Admin' when profile is null")
    void getAllAdmins_ShouldFallbackToSystemAdminWhenProfileMissing() {
        UUID rootId = UUID.randomUUID();
        Account rootAccount = Account.builder()
                .id(rootId)
                .email("root@edil.com")
                .role(AccountRole.ROOT_ADMIN)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(accountRepository.findAllByRoleIn(List.of(AccountRole.ADMIN, AccountRole.ROOT_ADMIN)))
                .thenReturn(List.of(rootAccount));
        when(adminProfileRepository.findByAccountId(rootId)).thenReturn(Optional.empty());

        List<AdminUserResponse> result = adminManagementService.getAllAdmins();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFullName()).isEqualTo("System Admin");
    }

    @Test
    @DisplayName("createAdmin should successfully create and persist admin account and profile")
    void createAdmin_ShouldCreateSuccessfully() {
        when(accountRepository.findByEmail(createAdminRequest.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(createAdminRequest.getPassword())).thenReturn("encodedAdminPass");

        UUID newAccountId = UUID.randomUUID();
        Account savedAccount = Account.builder()
                .id(newAccountId)
                .email(createAdminRequest.getEmail())
                .passwordHash("encodedAdminPass")
                .role(AccountRole.ADMIN)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        AdminUserResponse response = adminManagementService.createAdmin(createAdminRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccountId()).isEqualTo(newAccountId);
        assertThat(response.getEmail()).isEqualTo("newadmin@edil.com");
        assertThat(response.getFullName()).isEqualTo("Kaleb Admin");
        assertThat(response.getRole()).isEqualTo("ADMIN");
        assertThat(response.isActive()).isTrue();

        ArgumentCaptor<AdminProfile> profileCaptor = ArgumentCaptor.forClass(AdminProfile.class);
        verify(adminProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getFullName()).isEqualTo("Kaleb Admin");
        assertThat(profileCaptor.getValue().getAccount()).isEqualTo(savedAccount);
    }

    @Test
    @DisplayName("createAdmin should throw EmailAlreadyExistsException if email already taken")
    void createAdmin_ShouldThrowWhenEmailExists() {
        when(accountRepository.findByEmail(createAdminRequest.getEmail()))
                .thenReturn(Optional.of(new Account()));

        assertThatThrownBy(() -> adminManagementService.createAdmin(createAdminRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Email is already taken");

        verify(accountRepository, never()).save(any());
        verify(adminProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggleAdminStatus should successfully update active flag of ADMIN")
    void toggleAdminStatus_ShouldUpdateAdminStatus() {
        UUID accountId = UUID.randomUUID();
        Account adminAccount = Account.builder()
                .id(accountId)
                .email("admin@edil.com")
                .role(AccountRole.ADMIN)
                .isActive(true)
                .build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(adminAccount));
        when(accountRepository.findByEmail("admin@edil.com")).thenReturn(Optional.of(adminAccount));

        adminManagementService.toggleAdminStatus(accountId, false);

        assertThat(adminAccount.getIsActive()).isFalse();
        verify(accountRepository).save(adminAccount);
    }

    @Test
    @DisplayName("toggleAdminStatus should throw IllegalStateException when trying to alter ROOT_ADMIN status")
    void toggleAdminStatus_ShouldPreventAlteringRootAdmin() {
        UUID rootId = UUID.randomUUID();
        Account rootAccount = Account.builder()
                .id(rootId)
                .email("root@edil.com")
                .role(AccountRole.ROOT_ADMIN)
                .isActive(true)
                .build();

        when(accountRepository.findById(rootId)).thenReturn(Optional.of(rootAccount));
        when(accountRepository.findByEmail("root@edil.com")).thenReturn(Optional.of(rootAccount));

        assertThatThrownBy(() -> adminManagementService.toggleAdminStatus(rootId, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot alter Root Admin active status");

        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggleAdminStatus should throw AccountNotFoundException if account does not exist")
    void toggleAdminStatus_ShouldThrowWhenAccountNotFound() {
        UUID missingId = UUID.randomUUID();
        when(accountRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminManagementService.toggleAdminStatus(missingId, true))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Admin account not found");
    }
}
