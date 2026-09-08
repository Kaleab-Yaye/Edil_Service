package com.edil.service;

import com.edil.domain.Account;
import com.edil.domain.AdminProfile;
import com.edil.domain.CreatorProfile;
import com.edil.domain.UserProfile;
import com.edil.domain.enums.AccountRole;
import com.edil.dto.request.GetUserProfileRequest;
import com.edil.dto.response.UserMeResponse;
import com.edil.exception.AccountNotFoundException;
import com.edil.repository.AccountRepository;
import com.edil.repository.AdminProfileRepository;
import com.edil.repository.CreatorProfileRepository;
import com.edil.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
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

    public Account getAccountByEmail(String email) {
        log.info("the user  email that is being tried ot be fetched is {}", email);
       return accountRepository.findByEmail(email)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }


    @Cacheable(value = "user_email_to_UUID", key ="#email")

    public UUID getAccountIDByEmail(String email){
        log.info("cache missed for Email to UUID methode");
        return accountRepository.findByEmail(email).orElseThrow(()->new AccountNotFoundException("the account with the email "+email+ " is not found")).getId();
    }



    public ResponseEntity<UserMeResponse>  getUserDetails(GetUserProfileRequest getUserProfileRequest){

        UserProfile profile  = userProfileRepository.findById(getUserProfileRequest.userId()).orElseThrow(()->new AccountNotFoundException("the user profile with the id " +getUserProfileRequest.userId() + " is not found"));
        Account account =  profile.getAccount();

       if(!account.getRole().equals(AccountRole.USER )){
           return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
       }



        UserMeResponse.UserMeResponseBuilder builder = UserMeResponse.builder()
                .accountId(account.getId())
                .email(account.getEmail())
                .role(account.getRole().name())
                .active(Boolean.TRUE.equals(account.getIsActive()));

            builder.fullName(profile.getFullName())
                    .phoneNumber(profile.getPhoneNumber())
                    .address(profile.getAddress())
                    .refundBankAccount(profile.getRefundBankAccount());


            return ResponseEntity.status(HttpStatus.OK).body(builder.build());
    }
}
