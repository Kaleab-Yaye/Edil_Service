package com.edil.service;

import com.edil.domain.Account;
import com.edil.domain.AdminProfile;
import com.edil.domain.CreatorProfile;
import com.edil.domain.UserProfile;
import com.edil.domain.enums.AccountRole;
import com.edil.dto.response.UserMeResponse;
import com.edil.exception.AccountNotFoundException;
import com.edil.repository.AccountRepository;
import com.edil.repository.AdminProfileRepository;
import com.edil.repository.CreatorProfileRepository;
import com.edil.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final AdminProfileRepository adminProfileRepository;

    public UserMeResponse getMe(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        UserMeResponse.UserMeResponseBuilder builder = UserMeResponse.builder()
                .accountId(account.getId())
                .email(account.getEmail())
                .role(account.getRole().name())
                .active(Boolean.TRUE.equals(account.getIsActive()));

        if (account.getRole() == AccountRole.USER) {
            UserProfile profile = userProfileRepository.findByAccountId(account.getId()).orElse(null);
            if (profile != null) {
                builder.fullName(profile.getFullName())
                       .phoneNumber(profile.getPhoneNumber())
                       .address(profile.getAddress())
                       .refundBankAccount(profile.getRefundBankAccount());
            }
        } else if (account.getRole() == AccountRole.CREATOR) {
            CreatorProfile profile = creatorProfileRepository.findByAccountId(account.getId()).orElse(null);
            if (profile != null) {
                builder.fullName(profile.getFullName())
                       .phoneNumber(profile.getPhoneNumber())
                       .channelLink(profile.getChannelLink())
                       .aboutSection(profile.getAboutSection())
                       .payoutBankAccount(profile.getPayoutBankAccount())
                       .onboardingStatus(profile.getOnboardingStatus() != null ? profile.getOnboardingStatus().name() : null);
            }
        } else if (account.getRole() == AccountRole.ADMIN || account.getRole() == AccountRole.ROOT_ADMIN) {
            AdminProfile profile = adminProfileRepository.findByAccountId(account.getId()).orElse(null);
            if (profile != null) {
                builder.fullName(profile.getFullName());
            }
        }

        return builder.build();
    }
}
