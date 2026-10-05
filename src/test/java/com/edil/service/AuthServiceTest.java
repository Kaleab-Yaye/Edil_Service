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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private CreatorProfileRepository creatorProfileRepository;

    @Mock
    private CreatorOnboardingQueueRepository onboardingQueueRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterUserRequest userRequest;
    private RegisterCreatorRequest creatorRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        userRequest = new RegisterUserRequest();
        userRequest.setEmail("testuser@edil.com");
        userRequest.setPassword("securePassword123!");
        userRequest.setFullName("Abebe Bikila");
        userRequest.setPhoneNumber("+251911223344");
        userRequest.setAddress("Addis Ababa, Bole");
        userRequest.setRefundBankAccount("1000123456789");

        creatorRequest = new RegisterCreatorRequest();
        creatorRequest.setEmail("creator@edil.com");
        creatorRequest.setPassword("creatorPass456!");
        creatorRequest.setFullName("Elias Melka");
        creatorRequest.setPhoneNumber("+251922334455");
        creatorRequest.setChannelLink("https://youtube.com/@elias");
        creatorRequest.setAboutSection("Music producer and raffle host");
        creatorRequest.setPayoutBankAccount("1000987654321");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("testuser@edil.com");
        loginRequest.setPassword("securePassword123!");
    }

    @Nested
    @DisplayName("registerUser Tests")
    class RegisterUserTests {

        @Test
        @DisplayName("Should successfully register standard user, hash password, create profile, and return JWT")
        void shouldRegisterUserSuccessfully() {
            when(accountRepository.findByEmail(userRequest.getEmail())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(userRequest.getPassword())).thenReturn("encodedPasswordHash");

            Account savedAccount = Account.builder()
                    .id(UUID.randomUUID())
                    .email(userRequest.getEmail())
                    .passwordHash("encodedPasswordHash")
                    .role(AccountRole.USER)
                    .isActive(true)
                    .build();

            when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
            when(jwtService.generateToken(savedAccount)).thenReturn("jwt.token.value");

            AuthResponse response = authService.registerUser(userRequest);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt.token.value");

            // Verify account details
            ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(accountCaptor.capture());
            Account capturedAccount = accountCaptor.getValue();
            assertThat(capturedAccount.getEmail()).isEqualTo("testuser@edil.com");
            assertThat(capturedAccount.getPasswordHash()).isEqualTo("encodedPasswordHash");
            assertThat(capturedAccount.getRole()).isEqualTo(AccountRole.USER);
            assertThat(capturedAccount.getIsActive()).isTrue();

            // Verify user profile details
            ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
            verify(userProfileRepository).save(profileCaptor.capture());
            UserProfile capturedProfile = profileCaptor.getValue();
            assertThat(capturedProfile.getAccount()).isEqualTo(savedAccount);
            assertThat(capturedProfile.getFullName()).isEqualTo("Abebe Bikila");
            assertThat(capturedProfile.getPhoneNumber()).isEqualTo("+251911223344");
            assertThat(capturedProfile.getAddress()).isEqualTo("Addis Ababa, Bole");
            assertThat(capturedProfile.getRefundBankAccount()).isEqualTo("1000123456789");
        }

        @Test
        @DisplayName("Should throw EmailAlreadyExistsException if email is already registered")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            Account existing = Account.builder().email(userRequest.getEmail()).build();
            when(accountRepository.findByEmail(userRequest.getEmail())).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> authService.registerUser(userRequest))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessage("Email is already taken");

            verify(accountRepository, never()).save(any());
            verify(userProfileRepository, never()).save(any());
            verify(jwtService, never()).generateToken(any());
        }
    }

    @Nested
    @DisplayName("registerUserFromInternal Tests")
    class RegisterUserFromInternalTests {

        @Test
        @DisplayName("Should successfully create user profile internally and return saved Account")
        void shouldRegisterUserFromInternalSuccessfully() {
            when(accountRepository.findByEmail(userRequest.getEmail())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(userRequest.getPassword())).thenReturn("internalHash");

            Account savedAccount = Account.builder()
                    .id(UUID.randomUUID())
                    .email(userRequest.getEmail())
                    .passwordHash("internalHash")
                    .role(AccountRole.USER)
                    .isActive(true)
                    .build();

            when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

            Account result = authService.registerUserFromInternal(userRequest);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(userRequest.getEmail());
            verify(userProfileRepository).save(any(UserProfile.class));
            verify(jwtService, never()).generateToken(any()); // internal registration does not issue JWT
        }

        @Test
        @DisplayName("Should throw EmailAlreadyExistsException when registering internally with existing email")
        void shouldThrowExceptionWhenInternalEmailExists() {
            when(accountRepository.findByEmail(userRequest.getEmail())).thenReturn(Optional.of(new Account()));

            assertThatThrownBy(() -> authService.registerUserFromInternal(userRequest))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessage("Email is already taken");

            verify(accountRepository, never()).save(any());
            verify(userProfileRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("registerCreator Tests")
    class RegisterCreatorTests {

        @Test
        @DisplayName("Should successfully register creator with PENDING status, queue entry, and return JWT")
        void shouldRegisterCreatorSuccessfully() {
            when(accountRepository.findByEmail(creatorRequest.getEmail())).thenReturn(Optional.empty());
            when(creatorProfileRepository.findByPhoneNumber(creatorRequest.getPhoneNumber())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(creatorRequest.getPassword())).thenReturn("encodedCreatorPass");

            Account savedAccount = Account.builder()
                    .id(UUID.randomUUID())
                    .email(creatorRequest.getEmail())
                    .passwordHash("encodedCreatorPass")
                    .role(AccountRole.CREATOR)
                    .isActive(true)
                    .build();

            when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
            when(jwtService.generateToken(savedAccount)).thenReturn("jwt.creator.token");

            AuthResponse response = authService.registerCreator(creatorRequest);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt.creator.token");

            // Verify Creator Profile
            ArgumentCaptor<CreatorProfile> profileCaptor = ArgumentCaptor.forClass(CreatorProfile.class);
            verify(creatorProfileRepository).save(profileCaptor.capture());
            CreatorProfile savedProfile = profileCaptor.getValue();
            assertThat(savedProfile.getAccount()).isEqualTo(savedAccount);
            assertThat(savedProfile.getFullName()).isEqualTo("Elias Melka");
            assertThat(savedProfile.getPhoneNumber()).isEqualTo("+251922334455");
            assertThat(savedProfile.getChannelLink()).isEqualTo("https://youtube.com/@elias");
            assertThat(savedProfile.getAboutSection()).isEqualTo("Music producer and raffle host");
            assertThat(savedProfile.getPayoutBankAccount()).isEqualTo("1000987654321");
            assertThat(savedProfile.getOnboardingStatus()).isEqualTo(OnboardingStatus.PENDING);

            // Verify Onboarding Queue entry
            ArgumentCaptor<CreatorOnboardingQueue> queueCaptor = ArgumentCaptor.forClass(CreatorOnboardingQueue.class);
            verify(onboardingQueueRepository).save(queueCaptor.capture());
            assertThat(queueCaptor.getValue().getAccount()).isEqualTo(savedAccount);
        }

        @Test
        @DisplayName("Should throw EmailAlreadyExistsException if creator email is duplicate")
        void shouldThrowExceptionWhenCreatorEmailExists() {
            when(accountRepository.findByEmail(creatorRequest.getEmail())).thenReturn(Optional.of(new Account()));

            assertThatThrownBy(() -> authService.registerCreator(creatorRequest))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessage("Email is already taken");

            verify(creatorProfileRepository, never()).save(any());
            verify(onboardingQueueRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw PhoneAlreadyExistsException if creator phone is duplicate")
        void shouldThrowExceptionWhenCreatorPhoneExists() {
            when(accountRepository.findByEmail(creatorRequest.getEmail())).thenReturn(Optional.empty());
            when(creatorProfileRepository.findByPhoneNumber(creatorRequest.getPhoneNumber()))
                    .thenReturn(Optional.of(new CreatorProfile()));

            assertThatThrownBy(() -> authService.registerCreator(creatorRequest))
                    .isInstanceOf(PhoneAlreadyExistsException.class)
                    .hasMessage("Phone number is already taken");

            verify(accountRepository, never()).save(any());
            verify(creatorProfileRepository, never()).save(any());
            verify(onboardingQueueRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should successfully login when credentials are valid and account is active")
        void shouldLoginSuccessfully() {
            Account account = Account.builder()
                    .id(UUID.randomUUID())
                    .email(loginRequest.getEmail())
                    .passwordHash("hashedSecret")
                    .role(AccountRole.USER)
                    .isActive(true)
                    .build();

            when(accountRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(account));
            when(passwordEncoder.matches(loginRequest.getPassword(), "hashedSecret")).thenReturn(true);
            when(jwtService.generateToken(account)).thenReturn("valid.jwt.token");

            AuthResponse response = authService.login(loginRequest);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("valid.jwt.token");
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when email does not exist")
        void shouldThrowExceptionWhenEmailNotFound() {
            when(accountRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid email or password");

            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when password does not match")
        void shouldThrowExceptionWhenPasswordMismatch() {
            Account account = Account.builder()
                    .id(UUID.randomUUID())
                    .email(loginRequest.getEmail())
                    .passwordHash("hashedSecret")
                    .role(AccountRole.USER)
                    .isActive(true)
                    .build();

            when(accountRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(account));
            when(passwordEncoder.matches(loginRequest.getPassword(), "hashedSecret")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid email or password");

            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("Should throw AccountDeactivatedException when account isActive is false")
        void shouldThrowExceptionWhenAccountIsDeactivated() {
            Account deactivatedAccount = Account.builder()
                    .id(UUID.randomUUID())
                    .email(loginRequest.getEmail())
                    .passwordHash("hashedSecret")
                    .role(AccountRole.USER)
                    .isActive(false)
                    .build();

            when(accountRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(deactivatedAccount));
            when(passwordEncoder.matches(loginRequest.getPassword(), "hashedSecret")).thenReturn(true);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(AccountDeactivatedException.class)
                    .hasMessage("Account has been deactivated");

            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("Should throw AccountDeactivatedException when account isActive is null")
        void shouldThrowExceptionWhenAccountIsActiveIsNull() {
            Account nullActiveAccount = Account.builder()
                    .id(UUID.randomUUID())
                    .email(loginRequest.getEmail())
                    .passwordHash("hashedSecret")
                    .role(AccountRole.USER)
                    .isActive(null)
                    .build();

            when(accountRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(nullActiveAccount));
            when(passwordEncoder.matches(loginRequest.getPassword(), "hashedSecret")).thenReturn(true);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(AccountDeactivatedException.class)
                    .hasMessage("Account has been deactivated");

            verify(jwtService, never()).generateToken(any());
        }
    }
}
