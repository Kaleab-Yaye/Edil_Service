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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminManagementService {

    private final AccountRepository accountRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllAdmins() {
        List<AccountRole> adminRoles = List.of(AccountRole.ADMIN, AccountRole.ROOT_ADMIN);
        List<Account> adminAccounts = accountRepository.findAllByRoleIn(adminRoles);

        return adminAccounts.stream().map(account -> {
            AdminProfile profile = adminProfileRepository.findByAccountId(account.getId()).orElse(null);
            return AdminUserResponse.builder()
                    .accountId(account.getId())
                    .email(account.getEmail())
                    .fullName(profile != null ? profile.getFullName() : "System Admin")
                    .role(account.getRole().name())
                    .active(Boolean.TRUE.equals(account.getIsActive()))
                    .createdAt(account.getCreatedAt())
                    .build();
        }).toList();
    }

    @Transactional
    public AdminUserResponse createAdmin(CreateAdminRequest request) {
        if (accountRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email is already taken");
        }

        Account account = Account.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(AccountRole.ADMIN)
                .isActive(true)
                .build();
        account = accountRepository.save(account);

        AdminProfile profile = AdminProfile.builder()
                .account(account)
                .fullName(request.getFullName())
                .build();
        adminProfileRepository.save(profile);

        return AdminUserResponse.builder()
                .accountId(account.getId())
                .email(account.getEmail())
                .fullName(profile.getFullName())
                .role(account.getRole().name())
                .active(true)
                .createdAt(account.getCreatedAt())
                .build();
    }

    @Transactional
    public void toggleAdminStatus(UUID accountId, boolean active) {
        Account account = accountRepository.findByEmail(
                accountRepository.findById(accountId)
                        .orElseThrow(() -> new AccountNotFoundException("Admin account not found"))
                        .getEmail()
        ).orElseThrow(() -> new AccountNotFoundException("Admin account not found"));

        if (account.getRole() == AccountRole.ROOT_ADMIN) {
            throw new IllegalStateException("Cannot alter Root Admin active status");
        }

        account.setIsActive(active);
        accountRepository.save(account);
    }
}
