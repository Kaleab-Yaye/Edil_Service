package com.edil.service;

import com.edil.domain.Account;
import com.edil.domain.CreatorOnboardingQueue;
import com.edil.domain.CreatorProfile;
import com.edil.domain.UserProfile;
import com.edil.domain.enums.AccountRole;
import com.edil.domain.enums.OnboardingStatus;
import com.edil.dto.request.LoginRequest;
import com.edil.dto.request.RegisterCreatorRequest;
import com.edil.dto.request.RegisterUserRequest;
import com.edil.dto.response.AuthResponse;
import com.edil.exception.AccountDeactivatedException;
import com.edil.exception.EmailAlreadyExistsException;
import com.edil.exception.InvalidCredentialsException;
import com.edil.exception.PhoneAlreadyExistsException;
import com.edil.repository.AccountRepository;
import com.edil.repository.CreatorOnboardingQueueRepository;
import com.edil.repository.CreatorProfileRepository;
import com.edil.repository.UserProfileRepository;
import com.edil.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final CreatorOnboardingQueueRepository onboardingQueueRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse registerUser(RegisterUserRequest request) {
        if (accountRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email is already taken");
        }
        if (userProfileRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new PhoneAlreadyExistsException("Phone number is already taken");
        }

        Account account = Account.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(AccountRole.USER)
                .isActive(true)
                .build();
        account = accountRepository.save(account);

        UserProfile profile = UserProfile.builder()
                .account(account)
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .refundBankAccount(request.getRefundBankAccount())
                .build();
        userProfileRepository.save(profile);

        String token = jwtService.generateToken(account);
        return AuthResponse.builder().token(token).build();
    }


    public Account registerUserFromInternal(RegisterUserRequest request) {
        if (accountRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email is already taken");
        }
        if (userProfileRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new PhoneAlreadyExistsException("Phone number is already taken");
        }

        Account account = Account.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(AccountRole.USER)
                .isActive(true)
                .build();
        account = accountRepository.save(account);

        UserProfile profile = UserProfile.builder()
                .account(account)
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .refundBankAccount(request.getRefundBankAccount())
                .build();
        userProfileRepository.save(profile);

        return account;
    }




    public AuthResponse registerCreator(RegisterCreatorRequest request) {
        if (accountRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email is already taken");
        }
        if (creatorProfileRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new PhoneAlreadyExistsException("Phone number is already taken");
        }

        Account account = Account.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(AccountRole.CREATOR)
                .isActive(true)
                .build();
        account = accountRepository.save(account);

        CreatorProfile profile = CreatorProfile.builder()
                .account(account)
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .channelLink(request.getChannelLink())
                .aboutSection(request.getAboutSection())
                .payoutBankAccount(request.getPayoutBankAccount())
                .onboardingStatus(OnboardingStatus.PENDING)
                .build();
        creatorProfileRepository.save(profile);

        CreatorOnboardingQueue queueEntry = CreatorOnboardingQueue.builder()
                .account(account)
                .build();
        onboardingQueueRepository.save(queueEntry);

        String token = jwtService.generateToken(account);
        return AuthResponse.builder().token(token).build();
    }

    public AuthResponse login(LoginRequest request) {
        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!Boolean.TRUE.equals(account.getIsActive())) {
            throw new AccountDeactivatedException("Account has been deactivated");
        }

        String token = jwtService.generateToken(account);
        return AuthResponse.builder().token(token).build();
    }
}
