package com.edil.security;

import com.edil.domain.Account;
import com.edil.domain.enums.AccountRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private final String testSecret = "mySuperSecretKeyForTestingPurposesThatIsAtLeast256BitsLong1234567890";
    private final long testExpiration = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", testSecret);
        ReflectionTestUtils.setField(jwtService, "expiration", testExpiration);
    }

    @Test
    @DisplayName("generateToken and extractClaims should successfully encode and decode user claims")
    void shouldGenerateAndExtractUserClaims() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.builder()
                .id(accountId)
                .email("user@example.com")
                .role(AccountRole.USER)
                .isActive(true)
                .build();

        String token = jwtService.generateToken(account);

        assertThat(token).isNotBlank();
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).isEqualTo("user@example.com");

        Claims claims = jwtService.extractAllClaims(token);
        assertThat(claims.getSubject()).isEqualTo("user@example.com");
        assertThat(claims.get("accountId", String.class)).isEqualTo(accountId.toString());
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.get("isActive", Boolean.class)).isTrue();
    }

    @Test
    @DisplayName("generateToken should work for all account roles")
    void shouldGenerateTokenForAllRoles() {
        for (AccountRole role : AccountRole.values()) {
            Account account = Account.builder()
                    .id(UUID.randomUUID())
                    .email(role.name().toLowerCase() + "@edil.com")
                    .role(role)
                    .isActive(true)
                    .build();

            String token = jwtService.generateToken(account);
            Claims claims = jwtService.extractAllClaims(token);

            assertThat(claims.get("role", String.class)).isEqualTo(role.name());
            assertThat(jwtService.extractEmail(token)).isEqualTo(account.getEmail());
            assertThat(jwtService.isTokenValid(token)).isTrue();
        }
    }

    @Test
    @DisplayName("isTokenValid should return false when token has expired")
    void shouldReturnFalseForExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "expiration", -5000L); // expired 5 seconds ago

        Account account = Account.builder()
                .id(UUID.randomUUID())
                .email("expired@example.com")
                .role(AccountRole.USER)
                .isActive(true)
                .build();

        String expiredToken = jwtService.generateToken(account);

        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid should return false for invalid or malformed tokens")
    void shouldReturnFalseForMalformedTokens() {
        assertThat(jwtService.isTokenValid("")).isFalse();
        assertThat(jwtService.isTokenValid("not.a.valid.jwt.token")).isFalse();
        assertThat(jwtService.isTokenValid("randomString")).isFalse();
        assertThat(jwtService.isTokenValid(null)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid should return false when signed with different secret key")
    void shouldReturnFalseWhenSignedWithDifferentSecret() {
        JwtService anotherJwtService = new JwtService();
        ReflectionTestUtils.setField(anotherJwtService, "secret", "anotherDifferentSecretKeyWhichIsAlsoAtLeast256BitsLong1234567890!");
        ReflectionTestUtils.setField(anotherJwtService, "expiration", testExpiration);

        Account account = Account.builder()
                .id(UUID.randomUUID())
                .email("tampered@example.com")
                .role(AccountRole.USER)
                .isActive(true)
                .build();

        String tokenFromAnotherService = anotherJwtService.generateToken(account);

        assertThat(jwtService.isTokenValid(tokenFromAnotherService)).isFalse();
    }

    @Test
    @DisplayName("generateToken should encode inactive account flag properly")
    void shouldEncodeInactiveAccountFlag() {
        Account inactiveAccount = Account.builder()
                .id(UUID.randomUUID())
                .email("banned@example.com")
                .role(AccountRole.USER)
                .isActive(false)
                .build();

        String token = jwtService.generateToken(inactiveAccount);
        Claims claims = jwtService.extractAllClaims(token);

        assertThat(claims.get("isActive", Boolean.class)).isFalse();
    }
}
