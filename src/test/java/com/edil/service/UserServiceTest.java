package com.edil.service;

import com.edil.domain.Account;
import com.edil.domain.AdminProfile;
import com.edil.domain.CreatorProfile;
import com.edil.domain.UserProfile;
import com.edil.domain.enums.AccountRole;
import com.edil.domain.enums.OnboardingStatus;
import com.edil.dto.request.GetUserProfileRequest;
import com.edil.dto.response.UserMeResponse;
import com.edil.exception.AccountNotFoundException;
import com.edil.repository.AccountRepository;
import com.edil.repository.AdminProfileRepository;
import com.edil.repository.CreatorProfileRepository;
import com.edil.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private CreatorProfileRepository creatorProfileRepository;

    @Mock
    private AdminProfileRepository adminProfileRepository;

    @InjectMocks
    private UserService userService;

    private UUID accountId;
    private Account userAccount;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        userAccount = Account.builder()
                .id(accountId)
                .email("user@edil.com")
                .role(AccountRole.USER)
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("getMe Tests")
    class GetMeTests {

        @Test
        @DisplayName("Should return UserMeResponse for standard USER")
        void shouldReturnUserMeForStandardUser() {
            UserProfile profile = UserProfile.builder()
                    .id(UUID.randomUUID())
                    .account(userAccount)
                    .fullName("Almaz Ayana")
                    .phoneNumber("+251911998877")
                    .address("Hawassa")
                    .refundBankAccount("100011223344")
                    .build();

            when(accountRepository.findByEmail("user@edil.com")).thenReturn(Optional.of(userAccount));
            when(userProfileRepository.findByAccountId(accountId)).thenReturn(Optional.of(profile));

            UserMeResponse response = userService.getMe("user@edil.com");

            assertThat(response).isNotNull();
            assertThat(response.getAccountId()).isEqualTo(accountId);
            assertThat(response.getEmail()).isEqualTo("user@edil.com");
            assertThat(response.getFullName()).isEqualTo("Almaz Ayana");
            assertThat(response.getPhoneNumber()).isEqualTo("+251911998877");
            assertThat(response.getAddress()).isEqualTo("Hawassa");
            assertThat(response.getRefundBankAccount()).isEqualTo("100011223344");
            assertThat(response.getRole()).isEqualTo("USER");
            assertThat(response.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should return UserMeResponse for CREATOR with onboarding info")
        void shouldReturnUserMeForCreator() {
            Account creatorAccount = Account.builder()
                    .id(accountId)
                    .email("creator@edil.com")
                    .role(AccountRole.CREATOR)
                    .isActive(true)
                    .build();

            CreatorProfile creatorProfile = CreatorProfile.builder()
                    .id(UUID.randomUUID())
                    .account(creatorAccount)
                    .fullName("Dawit Tsige")
                    .phoneNumber("+251922001122")
                    .channelLink("https://youtube.com/@dawit")
                    .aboutSection("Music & Comedy")
                    .payoutBankAccount("100088889999")
                    .onboardingStatus(OnboardingStatus.ONBOARDED)
                    .build();

            when(accountRepository.findByEmail("creator@edil.com")).thenReturn(Optional.of(creatorAccount));
            when(creatorProfileRepository.findByAccountId(accountId)).thenReturn(Optional.of(creatorProfile));

            UserMeResponse response = userService.getMe("creator@edil.com");

            assertThat(response).isNotNull();
            assertThat(response.getRole()).isEqualTo("CREATOR");
            assertThat(response.getFullName()).isEqualTo("Dawit Tsige");
            assertThat(response.getChannelLink()).isEqualTo("https://youtube.com/@dawit");
            assertThat(response.getAboutSection()).isEqualTo("Music & Comedy");
            assertThat(response.getPayoutBankAccount()).isEqualTo("100088889999");
            assertThat(response.getOnboardingStatus()).isEqualTo("ONBOARDED");
        }

        @Test
        @DisplayName("Should return UserMeResponse for ADMIN")
        void shouldReturnUserMeForAdmin() {
            Account adminAccount = Account.builder()
                    .id(accountId)
                    .email("admin@edil.com")
                    .role(AccountRole.ADMIN)
                    .isActive(true)
                    .build();

            AdminProfile adminProfile = AdminProfile.builder()
                    .id(UUID.randomUUID())
                    .account(adminAccount)
                    .fullName("Admin User")
                    .build();

            when(accountRepository.findByEmail("admin@edil.com")).thenReturn(Optional.of(adminAccount));
            when(adminProfileRepository.findByAccountId(accountId)).thenReturn(Optional.of(adminProfile));

            UserMeResponse response = userService.getMe("admin@edil.com");

            assertThat(response).isNotNull();
            assertThat(response.getRole()).isEqualTo("ADMIN");
            assertThat(response.getFullName()).isEqualTo("Admin User");
        }

        @Test
        @DisplayName("getMe should throw AccountNotFoundException if email not found")
        void shouldThrowWhenAccountNotFound() {
            when(accountRepository.findByEmail("unknown@edil.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getMe("unknown@edil.com"))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessage("Account not found");
        }
    }

    @Nested
    @DisplayName("Account Lookup Tests")
    class AccountLookupTests {

        @Test
        @DisplayName("getAccountByEmail should return Account when found")
        void shouldReturnAccountByEmail() {
            when(accountRepository.findByEmail("user@edil.com")).thenReturn(Optional.of(userAccount));

            Account result = userService.getAccountByEmail("user@edil.com");
            assertThat(result).isEqualTo(userAccount);
        }

        @Test
        @DisplayName("getAccountIDByEmail should return UUID when found")
        void shouldReturnAccountIdByEmail() {
            when(accountRepository.findByEmail("user@edil.com")).thenReturn(Optional.of(userAccount));

            UUID id = userService.getAccountIDByEmail("user@edil.com");
            assertThat(id).isEqualTo(accountId);
        }

        @Test
        @DisplayName("getAccountIDByEmail should throw AccountNotFoundException when not found")
        void shouldThrowWhenIdNotFound() {
            when(accountRepository.findByEmail("ghost@edil.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getAccountIDByEmail("ghost@edil.com"))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining("ghost@edil.com");
        }
    }

    @Nested
    @DisplayName("getUserDetails Tests")
    class UserDetailsTests {

        @Test
        @DisplayName("getUserDetails should return 200 OK for valid USER profile")
        void shouldReturnUserDetailsForUserRole() {
            UUID profileId = UUID.randomUUID();
            UserProfile profile = UserProfile.builder()
                    .id(profileId)
                    .account(userAccount)
                    .fullName("Selamawit Yohannes")
                    .phoneNumber("+251933445566")
                    .address("Gondar")
                    .refundBankAccount("100066778899")
                    .build();

            when(userProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));

            ResponseEntity<UserMeResponse> response = userService.getUserDetails(new GetUserProfileRequest(profileId));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getFullName()).isEqualTo("Selamawit Yohannes");
            assertThat(response.getBody().getRefundBankAccount()).isEqualTo("100066778899");
        }

        @Test
        @DisplayName("getUserDetails should return 404 NOT_FOUND if account role is not USER")
        void shouldReturnNotFoundForNonRoleUser() {
            UUID profileId = UUID.randomUUID();
            Account creatorAccount = Account.builder()
                    .id(UUID.randomUUID())
                    .email("creator@edil.com")
                    .role(AccountRole.CREATOR)
                    .build();

            UserProfile profile = UserProfile.builder()
                    .id(profileId)
                    .account(creatorAccount)
                    .build();

            when(userProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));

            ResponseEntity<UserMeResponse> response = userService.getUserDetails(new GetUserProfileRequest(profileId));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
