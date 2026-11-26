package com.hrm.saas.domain.auth.service;

import com.hrm.saas.common.exception.AppException;
import com.hrm.saas.config.JwtProperties;
import com.hrm.saas.domain.auth.dto.request.LoginRequest;
import com.hrm.saas.domain.auth.dto.request.RegisterRequest;
import com.hrm.saas.domain.auth.dto.request.ResetPasswordRequest;
import com.hrm.saas.domain.auth.dto.request.VerifyEmailRequest;
import com.hrm.saas.domain.auth.dto.response.AuthResult;
import com.hrm.saas.domain.auth.dto.response.TenantMembershipResponse;
import com.hrm.saas.domain.auth.dto.response.UserResponse;
import com.hrm.saas.domain.auth.entity.AuthActionToken;
import com.hrm.saas.domain.auth.entity.RefreshToken;
import com.hrm.saas.domain.auth.entity.TenantMembership;
import com.hrm.saas.domain.auth.entity.User;
import com.hrm.saas.domain.auth.enums.ActionTokenType;
import com.hrm.saas.domain.auth.enums.MembershipStatus;
import com.hrm.saas.domain.auth.enums.TokenType;
import com.hrm.saas.domain.auth.mapper.AuthMapper;
import com.hrm.saas.domain.auth.repository.AuthActionTokenRepository;
import com.hrm.saas.domain.auth.repository.RefreshTokenRepository;
import com.hrm.saas.domain.auth.repository.TenantMembershipRepository;
import com.hrm.saas.domain.auth.repository.UserRepository;
import com.hrm.saas.domain.auth.security.JwtTokenProvider;
import com.hrm.saas.domain.auth.service.impl.AuthServiceImpl;
import com.hrm.saas.domain.tenant.entity.Tenant;
import com.hrm.saas.domain.tenant.enums.TenantStatus;
import com.hrm.saas.domain.tenant.repository.TenantRepository;
import com.hrm.saas.domain.tenant.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl — Unit Tests")
class AuthServiceImplTest {

        @Mock
        private UserRepository userRepository;
        @Mock
        private TenantMembershipRepository membershipRepository;
        @Mock
        private RefreshTokenRepository refreshTokenRepository;
        @Mock
        private AuthActionTokenRepository actionTokenRepository;
        @Mock
        private TenantRepository tenantRepository;
        @Mock
        private TenantService tenantService;
        @Spy
        private AuthMapper authMapper = new AuthMapper();
        @Mock
        private PasswordEncoder passwordEncoder;
        @Mock
        private JwtTokenProvider jwtTokenProvider;
        @Spy
        private JwtProperties jwtProperties = new JwtProperties();

        @InjectMocks
        private AuthServiceImpl authService;

        private User sampleUser;
        private Tenant sampleTenant;
        private TenantMembership sampleMembership;

        @BeforeEach
        void setUp() {
                jwtProperties.setAccessTokenExpiration(Duration.ofMinutes(15));
                jwtProperties.setRefreshTokenExpiration(Duration.ofDays(7));

                sampleUser = User.builder()
                                .id(UUID.randomUUID())
                                .email("john@example.com")
                                .passwordHash("hashed-pw")
                                .firstName("John")
                                .lastName("Doe")
                                .isEmailVerified(true)
                                .isActive(true)
                                .failedLoginAttempts(0)
                                .build();

                sampleTenant = Tenant.builder()
                                .id(UUID.randomUUID())
                                .name("Acme Corp")
                                .slug("acme")
                                .status(TenantStatus.ACTIVE)
                                .build();

                sampleMembership = TenantMembership.builder()
                                .id(UUID.randomUUID())
                                .user(sampleUser)
                                .tenant(sampleTenant)
                                .isOwner(true)
                                .roles(new java.util.HashSet<>())
                                .status(MembershipStatus.ACTIVE)
                                .joinedAt(Instant.now())
                                .build();
        }

        @Nested
        @DisplayName("register()")
        class RegisterTests {

                @Test
                @DisplayName("Should register user without company and issue Account-Scoped token")
                void shouldRegisterUserWithoutCompany() {
                        RegisterRequest request = RegisterRequest.builder()
                                        .email("john@example.com")
                                        .password("secret123")
                                        .firstName("John")
                                        .lastName("Doe")
                                        .build();

                        when(userRepository.existsByEmailIgnoreCaseAndIsDeletedFalse("john@example.com"))
                                        .thenReturn(false);
                        when(passwordEncoder.encode("secret123")).thenReturn("hashed-pw");
                        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
                        when(jwtTokenProvider.generateAccountScopedToken(eq(sampleUser.getId()),
                                        eq("john@example.com")))
                                        .thenReturn("account-scoped-jwt");

                        AuthResult result = authService.register(request, "JUnit", "127.0.0.1");

                        assertNotNull(result);
                        assertEquals("account-scoped-jwt", result.getAuthResponse().getAccessToken());
                        assertEquals(TokenType.ACCOUNT_SCOPED, result.getAuthResponse().getTokenType());
                        assertNull(result.getAuthResponse().getActiveTenant());
                        assertNotNull(result.getRawRefreshToken());
                        verify(refreshTokenRepository).save(any(RefreshToken.class));
                }

                @Test
                @DisplayName("Should throw 409 Conflict when email already registered")
                void shouldThrowConflictWhenEmailExists() {
                        RegisterRequest request = RegisterRequest.builder()
                                        .email("john@example.com")
                                        .password("secret123")
                                        .build();

                        when(userRepository.existsByEmailIgnoreCaseAndIsDeletedFalse("john@example.com"))
                                        .thenReturn(true);

                        AppException ex = assertThrows(AppException.class,
                                        () -> authService.register(request, "JUnit", "127.0.0.1"));
                        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
                        verify(userRepository, never()).save(any());
                }
        }

        @Nested
        @DisplayName("login()")
        class LoginTests {

                @Test
                @DisplayName("Should successfully login and auto-select tenant when user has 1 membership")
                void shouldAutoSelectTenantForSingleMembership() {
                        LoginRequest request = LoginRequest.builder()
                                        .email("john@example.com")
                                        .password("correct-pw")
                                        .build();

                        when(userRepository.findByEmailIgnoreCaseAndIsDeletedFalse("john@example.com"))
                                        .thenReturn(Optional.of(sampleUser));
                        when(passwordEncoder.matches("correct-pw", "hashed-pw")).thenReturn(true);
                        when(membershipRepository.findAllByUserIdAndStatusWithTenant(sampleUser.getId(),
                                        MembershipStatus.ACTIVE))
                                        .thenReturn(List.of(sampleMembership));
                        when(jwtTokenProvider.generateTenantScopedToken(any(), any(), any(), any(), any(), any()))
                                        .thenReturn("tenant-scoped-jwt");

                        AuthResult result = authService.login(request, "JUnit", "127.0.0.1");

                        assertNotNull(result);
                        assertEquals(TokenType.TENANT_SCOPED, result.getAuthResponse().getTokenType());
                        assertNotNull(result.getAuthResponse().getActiveTenant());
                        assertEquals("acme", result.getAuthResponse().getActiveTenant().getTenantSlug());
                        assertEquals(0, sampleUser.getFailedLoginAttempts());
                }

                @Test
                @DisplayName("Should return Account-Scoped token when user belongs to multiple companies")
                void shouldReturnAccountScopedWhenMultipleCompanies() {
                        LoginRequest request = LoginRequest.builder()
                                        .email("john@example.com")
                                        .password("correct-pw")
                                        .build();

                        Tenant secondTenant = Tenant.builder().id(UUID.randomUUID()).name("Beta").slug("beta")
                                        .status(TenantStatus.ACTIVE).build();
                        TenantMembership secondMembership = TenantMembership.builder()
                                        .id(UUID.randomUUID()).user(sampleUser).tenant(secondTenant).isOwner(false)
                                        .roles(new java.util.HashSet<>()).status(MembershipStatus.ACTIVE).build();

                        when(userRepository.findByEmailIgnoreCaseAndIsDeletedFalse("john@example.com"))
                                        .thenReturn(Optional.of(sampleUser));
                        when(passwordEncoder.matches("correct-pw", "hashed-pw")).thenReturn(true);
                        when(membershipRepository.findAllByUserIdAndStatusWithTenant(sampleUser.getId(),
                                        MembershipStatus.ACTIVE))
                                        .thenReturn(List.of(sampleMembership, secondMembership));
                        when(jwtTokenProvider.generateAccountScopedToken(any(), any()))
                                        .thenReturn("account-scoped-jwt");

                        AuthResult result = authService.login(request, "JUnit", "127.0.0.1");

                        assertNotNull(result);
                        assertEquals(TokenType.ACCOUNT_SCOPED, result.getAuthResponse().getTokenType());
                        assertNull(result.getAuthResponse().getActiveTenant());
                        assertEquals(2, result.getAuthResponse().getAvailableTenants().size());
                }

                @Test
                @DisplayName("Should fail with 401 and increment failed attempts on invalid password")
                void shouldIncrementFailedAttemptsOnInvalidPassword() {
                        LoginRequest request = LoginRequest.builder()
                                        .email("john@example.com")
                                        .password("wrong-pw")
                                        .build();

                        when(userRepository.findByEmailIgnoreCaseAndIsDeletedFalse("john@example.com"))
                                        .thenReturn(Optional.of(sampleUser));
                        when(passwordEncoder.matches("wrong-pw", "hashed-pw")).thenReturn(false);

                        AppException ex = assertThrows(AppException.class,
                                        () -> authService.login(request, "JUnit", "127.0.0.1"));
                        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
                        assertEquals(1, sampleUser.getFailedLoginAttempts());
                        verify(userRepository).save(sampleUser);
                }

                @Test
                @DisplayName("Should lock account after 5 failed login attempts")
                void shouldLockAccountAfter5FailedAttempts() {
                        sampleUser.setFailedLoginAttempts(4); // 5th attempt incoming

                        LoginRequest request = LoginRequest.builder()
                                        .email("john@example.com")
                                        .password("wrong-pw")
                                        .build();

                        when(userRepository.findByEmailIgnoreCaseAndIsDeletedFalse("john@example.com"))
                                        .thenReturn(Optional.of(sampleUser));
                        when(passwordEncoder.matches("wrong-pw", "hashed-pw")).thenReturn(false);

                        assertThrows(AppException.class, () -> authService.login(request, "JUnit", "127.0.0.1"));
                        assertTrue(sampleUser.isAccountLocked());
                        assertNotNull(sampleUser.getLockedUntil());
                }
        }

        @Nested
        @DisplayName("refresh() — RTR & Replay Detection")
        class RefreshTokenRotationTests {

                @Test
                @DisplayName("RTR Happy Path: marks old token used, creates new token under same family")
                void shouldRotateRefreshTokenSuccessfully() {
                        UUID familyId = UUID.randomUUID();
                        RefreshToken oldToken = RefreshToken.builder()
                                        .id(UUID.randomUUID())
                                        .user(sampleUser)
                                        .tenant(sampleTenant)
                                        .familyId(familyId)
                                        .tokenHash("hash123")
                                        .isUsed(false)
                                        .isRevoked(false)
                                        .expiresAt(Instant.now().plus(Duration.ofDays(7)))
                                        .build();

                        when(refreshTokenRepository.findByTokenHashWithUserAndTenant(anyString()))
                                        .thenReturn(Optional.of(oldToken));
                        when(membershipRepository.findAllByUserIdAndStatusWithTenant(sampleUser.getId(),
                                        MembershipStatus.ACTIVE))
                                        .thenReturn(List.of(sampleMembership));
                        when(jwtTokenProvider.generateTenantScopedToken(any(), any(), any(), any(), any(), any()))
                                        .thenReturn("new-tenant-jwt");

                        AuthResult result = authService.refresh("raw-valid-token", "JUnit", "127.0.0.1");

                        assertNotNull(result);
                        assertTrue(oldToken.getIsUsed(), "Old token must be marked as used");
                        verify(refreshTokenRepository).save(oldToken);
                        verify(refreshTokenRepository).save(argThat(
                                        newToken -> newToken.getFamilyId().equals(familyId) && !newToken.getIsUsed()));
                }

                @Test
                @DisplayName("RTR Breach Detection: Reusing an already-used token revokes the entire family")
                void shouldRevokeFamilyOnReplayAttack() {
                        UUID familyId = UUID.randomUUID();
                        RefreshToken reusedToken = RefreshToken.builder()
                                        .id(UUID.randomUUID())
                                        .user(sampleUser)
                                        .familyId(familyId)
                                        .isUsed(true) // already consumed!
                                        .isRevoked(false)
                                        .expiresAt(Instant.now().plus(Duration.ofDays(7)))
                                        .build();

                        when(refreshTokenRepository.findByTokenHashWithUserAndTenant(anyString()))
                                        .thenReturn(Optional.of(reusedToken));

                        AppException ex = assertThrows(AppException.class,
                                        () -> authService.refresh("stolen-replayed-token", "Attacker", "192.168.1.1"));

                        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
                        assertTrue(ex.getMessage().contains("re-use detected"));
                        verify(refreshTokenRepository).revokeFamily(familyId);
                }
        }

        @Nested
        @DisplayName("switchTenant()")
        class SwitchTenantTests {

                @Test
                @DisplayName("Should successfully switch tenant when user is active member")
                void shouldSwitchTenantSuccessfully() {
                        UUID targetTenantId = sampleTenant.getId();

                        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
                        when(membershipRepository.findByUserIdAndTenantIdWithTenant(sampleUser.getId(), targetTenantId))
                                        .thenReturn(Optional.of(sampleMembership));
                        when(membershipRepository.findAllByUserIdAndStatusWithTenant(sampleUser.getId(),
                                        MembershipStatus.ACTIVE))
                                        .thenReturn(List.of(sampleMembership));
                        when(jwtTokenProvider.generateTenantScopedToken(any(), any(), any(), any(), any(), any()))
                                        .thenReturn("switched-tenant-jwt");

                        AuthResult result = authService.switchTenant(sampleUser.getId(), targetTenantId, "JUnit",
                                        "127.0.0.1");

                        assertNotNull(result);
                        assertEquals("switched-tenant-jwt", result.getAuthResponse().getAccessToken());
                        assertEquals(TokenType.TENANT_SCOPED, result.getAuthResponse().getTokenType());
                }

                @Test
                @DisplayName("Should throw 403 Forbidden when tenant is suspended")
                void shouldThrowForbiddenWhenTenantSuspended() {
                        sampleTenant.setStatus(TenantStatus.SUSPENDED);

                        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
                        when(membershipRepository.findByUserIdAndTenantIdWithTenant(sampleUser.getId(),
                                        sampleTenant.getId()))
                                        .thenReturn(Optional.of(sampleMembership));

                        AppException ex = assertThrows(AppException.class,
                                        () -> authService.switchTenant(sampleUser.getId(), sampleTenant.getId(),
                                                        "JUnit", "127.0.0.1"));

                        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
                        assertTrue(ex.getMessage().contains("suspended"));
                }
        }

        @Nested
        @DisplayName("Action Tokens: Password Reset & Email Verification")
        class ActionTokenTests {

                @Test
                @DisplayName("Should verify email when valid token provided")
                void shouldVerifyEmailSuccessfully() {
                        sampleUser.setIsEmailVerified(false);

                        AuthActionToken actionToken = AuthActionToken.builder()
                                        .id(UUID.randomUUID())
                                        .user(sampleUser)
                                        .tokenType(ActionTokenType.EMAIL_VERIFICATION)
                                        .isUsed(false)
                                        .expiresAt(Instant.now().plusSeconds(3600))
                                        .build();

                        when(actionTokenRepository.findByTokenHashAndTokenTypeWithUser(anyString(),
                                        eq(ActionTokenType.EMAIL_VERIFICATION)))
                                        .thenReturn(Optional.of(actionToken));

                        authService.verifyEmail(new VerifyEmailRequest("valid-token"));

                        assertTrue(sampleUser.getIsEmailVerified());
                        assertTrue(actionToken.getIsUsed());
                        verify(userRepository).save(sampleUser);
                        verify(actionTokenRepository).save(actionToken);
                }

                @Test
                @DisplayName("Should reset password and revoke all user sessions")
                void shouldResetPasswordAndRevokeSessions() {
                        AuthActionToken actionToken = AuthActionToken.builder()
                                        .id(UUID.randomUUID())
                                        .user(sampleUser)
                                        .tokenType(ActionTokenType.PASSWORD_RESET)
                                        .isUsed(false)
                                        .expiresAt(Instant.now().plusSeconds(3600))
                                        .build();

                        when(actionTokenRepository.findByTokenHashAndTokenTypeWithUser(anyString(),
                                        eq(ActionTokenType.PASSWORD_RESET)))
                                        .thenReturn(Optional.of(actionToken));
                        when(passwordEncoder.encode("new-secret")).thenReturn("new-hashed-pw");

                        authService.resetPassword(new ResetPasswordRequest("token123", "new-secret"));

                        assertEquals("new-hashed-pw", sampleUser.getPasswordHash());
                        assertTrue(actionToken.getIsUsed());
                        verify(refreshTokenRepository).revokeAllByUserId(sampleUser.getId());
                }
        }
}
