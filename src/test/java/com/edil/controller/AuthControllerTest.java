package com.edil.controller;

import com.edil.config.SecurityConfig;
import com.edil.dto.request.LoginRequest;
import com.edil.dto.request.RegisterCreatorRequest;
import com.edil.dto.request.RegisterUserRequest;
import com.edil.dto.response.AuthResponse;
import com.edil.exception.AccountDeactivatedException;
import com.edil.exception.EmailAlreadyExistsException;
import com.edil.exception.GlobalExceptionHandler;
import com.edil.exception.InvalidCredentialsException;
import com.edil.repository.AccountRepository;
import com.edil.security.JwtAuthenticationFilter;
import com.edil.security.JwtService;
import com.edil.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AccountRepository accountRepository;

    @Nested
    @DisplayName("POST /api/auth/register/user")
    class RegisterUserTests {

        @Test
        @DisplayName("Should successfully register user and return 200 OK with AuthResponse")
        void registerUser_Success() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("john@example.com");
            request.setPassword("ValidPassword1!");
            request.setFullName("John");
            request.setPhoneNumber("0911223344");
            request.setRefundBankAccount("1000123456");

            AuthResponse expectedResponse = new AuthResponse("jwt-token");
            when(authService.registerUser(any(RegisterUserRequest.class))).thenReturn(expectedResponse);

            mockMvc.perform(post("/api/auth/register/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token"));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when required fields are missing or blank")
        void registerUser_InvalidPayload_BlankFields() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("");
            request.setPassword("");
            request.setFullName("");
            request.setPhoneNumber("");
            request.setRefundBankAccount("");

            mockMvc.perform(post("/api/auth/register/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when password length is less than 6")
        void registerUser_ShortPassword() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("john@example.com");
            request.setPassword("123");
            request.setFullName("John");
            request.setPhoneNumber("0911223344");
            request.setRefundBankAccount("1000123456");

            mockMvc.perform(post("/api/auth/register/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 409 Conflict when email already exists")
        void registerUser_EmailAlreadyExists() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("existing@example.com");
            request.setPassword("ValidPassword1!");
            request.setFullName("John");
            request.setPhoneNumber("0911223344");
            request.setRefundBankAccount("1000123456");

            when(authService.registerUser(any(RegisterUserRequest.class)))
                    .thenThrow(new EmailAlreadyExistsException("Email already registered"));

            mockMvc.perform(post("/api/auth/register/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"))
                    .andExpect(jsonPath("$.message").value("Email already registered"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/register/creator")
    class RegisterCreatorTests {

        @Test
        @DisplayName("Should successfully register creator and return 200 OK")
        void registerCreator_Success() throws Exception {
            RegisterCreatorRequest request = new RegisterCreatorRequest();
            request.setEmail("creator@example.com");
            request.setPassword("ValidPassword1!");
            request.setFullName("CreatorChannel");
            request.setPhoneNumber("0911223344");
            request.setChannelLink("https://youtube.com/channel");
            request.setAboutSection("Bio content");
            request.setPayoutBankAccount("1000123456");

            AuthResponse expectedResponse = new AuthResponse("jwt-token-creator");
            when(authService.registerCreator(any(RegisterCreatorRequest.class))).thenReturn(expectedResponse);

            mockMvc.perform(post("/api/auth/register/creator")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token-creator"));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when required creator fields are invalid")
        void registerCreator_InvalidPayload() throws Exception {
            RegisterCreatorRequest request = new RegisterCreatorRequest();
            request.setEmail("");
            request.setPassword("");
            request.setFullName("");

            mockMvc.perform(post("/api/auth/register/creator")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should return 200 OK with AuthResponse on valid credentials")
        void login_Success() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("user@example.com");
            request.setPassword("Password123!");

            AuthResponse expectedResponse = new AuthResponse("login-jwt-token");
            when(authService.login(any(LoginRequest.class))).thenReturn(expectedResponse);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("login-jwt-token"));
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when credentials are invalid")
        void login_InvalidCredentials() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("user@example.com");
            request.setPassword("WrongPassword!");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new InvalidCredentialsException("Invalid email or password"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        @Test
        @DisplayName("Should return 403 Forbidden when account is deactivated")
        void login_AccountDeactivated() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("deactivated@example.com");
            request.setPassword("Password123!");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new AccountDeactivatedException("Account is deactivated"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCOUNT_DEACTIVATED"))
                    .andExpect(jsonPath("$.message").value("Account is deactivated"));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when login request body is missing or invalid")
        void login_InvalidBody() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("");
            request.setPassword("");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
