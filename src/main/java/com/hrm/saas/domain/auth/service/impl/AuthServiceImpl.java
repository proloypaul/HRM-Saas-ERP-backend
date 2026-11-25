package com.hrm.saas.domain.auth.service.impl;

import com.hrm.saas.common.exception.AppException;
import com.hrm.saas.config.JwtProperties;
import com.hrm.saas.domain.auth.dto.request.*;
import com.hrm.saas.domain.auth.dto.response.*;
import com.hrm.saas.domain.auth.entity.AuthActionToken;
import com.hrm.saas.domain.auth.entity.RefreshToken;
import com.hrm.saas.domain.auth.entity.TenantMembership;
import com.hrm.saas.domain.auth.entity.User;
import com.hrm.saas.domain.auth.enums.ActionTokenType;
import com.hrm.saas.domain.auth.enums.MembershipRole;
import com.hrm.saas.domain.auth.enums.MembershipStatus;
import com.hrm.saas.domain.auth.enums.TokenType;
import com.hrm.saas.domain.auth.mapper.AuthMapper;
import com.hrm.saas.domain.auth.repository.AuthActionTokenRepository;
import com.hrm.saas.domain.auth.repository.RefreshTokenRepository;
import com.hrm.saas.domain.auth.repository.TenantMembershipRepository;
import com.hrm.saas.domain.auth.repository.UserRepository;
import com.hrm.saas.domain.auth.security.JwtTokenProvider;
import com.hrm.saas.domain.auth.security.TokenHashUtils;
import com.hrm.saas.domain.auth.service.AuthService;
import com.hrm.saas.domain.rbac.constants.RbacPermissions;
import com.hrm.saas.domain.rbac.entity.Role;
import com.hrm.saas.domain.rbac.service.PermissionCatalogSeeder;
import com.hrm.saas.domain.tenant.dto.request.CreateTenantRequest;
import com.hrm.saas.domain.tenant.dto.response.TenantResponse;
import com.hrm.saas.domain.tenant.entity.Tenant;
import com.hrm.saas.domain.tenant.enums.TenantStatus;
import com.hrm.saas.domain.tenant.repository.TenantRepository;
import com.hrm.saas.domain.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TenantMembershipRepository membershipRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthActionTokenRepository actionTokenRepository;
    private final TenantRepository tenantRepository;
    private final TenantService tenantService;
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final PermissionCatalogSeeder permissionCatalogSeeder;

    @Override
    @Transactional
    public AuthResult register(RegisterRequest request, String userAgent, String ipAddress) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCaseAndIsDeletedFalse(email)) {
            throw new AppException("User with email '" + email + "' already exists", HttpStatus.CONFLICT);
        }

        User user = authMapper.toEntity(request, passwordEncoder.encode(request.getPassword()));
        User savedUser = userRepository.save(user);
        log.info("Registered new user with id: {}", savedUser.getId());

        TenantMembership activeMembership = null;

        // If company details are provided, register the company and make the user the OWNER
        if (request.getCompanyName() != null && !request.getCompanyName().isBlank()) {
            String slug = request.getCompanySlug() != null && !request.getCompanySlug().isBlank()
                    ? request.getCompanySlug().trim().toLowerCase()
                    : generateSlugFromName(request.getCompanyName());

            TenantResponse tenantResponse = tenantService.createTenant(CreateTenantRequest.builder()
                    .name(request.getCompanyName().trim())
                    .slug(slug)
                    .companyEmail(email)
                    .phone(request.getPhone())
                    .build());

            Tenant tenant = tenantRepository.findById(tenantResponse.getId())
                    .orElseThrow(() -> new AppException("Tenant not found after creation", HttpStatus.INTERNAL_SERVER_ERROR));

            // Seed RBAC system roles for this tenant
            List<Role> seededRoles = permissionCatalogSeeder.seedSystemRolesForTenant(tenant);
            Role tenantAdminRole = seededRoles.stream()
                    .filter(r -> RbacPermissions.ROLE_TENANT_ADMIN.equals(r.getCode()))
                    .findFirst()
                    .orElse(null);

            Set<Role> assignedRoles = tenantAdminRole != null ? new java.util.HashSet<>(List.of(tenantAdminRole)) : new java.util.HashSet<>();

            TenantMembership membership = TenantMembership.builder()
                    .user(savedUser)
                    .tenant(tenant)
                    .role(MembershipRole.OWNER)
                    .roles(assignedRoles)
                    .status(MembershipStatus.ACTIVE)
                    .build();

            activeMembership = membershipRepository.save(membership);
            log.info("Assigned user '{}' as OWNER of tenant '{}'", savedUser.getId(), tenant.getId());
        }

        // Issue initial refresh token and access token
        UUID familyId = UUID.randomUUID();
        Tenant tenantForToken = activeMembership != null ? activeMembership.getTenant() : null;
        String rawRefreshToken = createRefreshToken(savedUser, tenantForToken, familyId, userAgent, ipAddress);

        List<TenantMembership> initialMemberships = activeMembership != null ? List.of(activeMembership) : List.of();
        AuthResponse authResponse = buildAuthResponse(savedUser, activeMembership, initialMemberships);
        return AuthResult.builder()
                .authResponse(authResponse)
                .rawRefreshToken(rawRefreshToken)
                .build();
    }

    @Override
    @Transactional
    public AuthResult login(LoginRequest request, String userAgent, String ipAddress) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCaseAndIsDeletedFalse(email)
                .orElseThrow(() -> new AppException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (user.isAccountLocked()) {
            throw new AppException("Account is temporarily locked due to multiple failed attempts. Please try again later.", HttpStatus.LOCKED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.incrementFailedAttempts();
            userRepository.save(user);
            throw new AppException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        user.resetFailedAttempts();
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // Fetch active memberships
        List<TenantMembership> activeMemberships = membershipRepository.findAllByUserIdAndStatusWithTenant(
                user.getId(), MembershipStatus.ACTIVE);

        TenantMembership selectedMembership = null;

        if (request.getTenantId() != null) {
            selectedMembership = activeMemberships.stream()
                    .filter(m -> m.getTenant().getId().equals(request.getTenantId()))
                    .findFirst()
                    .orElseThrow(() -> new AppException("You do not have active membership in the selected company", HttpStatus.FORBIDDEN));
        } else if (activeMemberships.size() == 1) {
            selectedMembership = activeMemberships.get(0);
        }

        // Validate selected tenant status
        if (selectedMembership != null && selectedMembership.getTenant().getStatus() != TenantStatus.ACTIVE) {
            throw new AppException("Selected company is " + selectedMembership.getTenant().getStatus().name().toLowerCase(), HttpStatus.FORBIDDEN);
        }

        UUID familyId = UUID.randomUUID();
        Tenant tenantForToken = selectedMembership != null ? selectedMembership.getTenant() : null;
        String rawRefreshToken = createRefreshToken(user, tenantForToken, familyId, userAgent, ipAddress);

        AuthResponse authResponse = buildAuthResponse(user, selectedMembership, activeMemberships);
        return AuthResult.builder()
                .authResponse(authResponse)
                .rawRefreshToken(rawRefreshToken)
                .build();
    }

    @Override
    @Transactional
    public AuthResult refresh(String rawRefreshToken, String userAgent, String ipAddress) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AppException("Refresh token missing", HttpStatus.UNAUTHORIZED);
        }

        String tokenHash = TokenHashUtils.hashToken(rawRefreshToken);
        RefreshToken existingToken = refreshTokenRepository.findByTokenHashWithUserAndTenant(tokenHash)
                .orElseThrow(() -> new AppException("Invalid refresh token", HttpStatus.UNAUTHORIZED));

        if (Boolean.TRUE.equals(existingToken.getIsRevoked())) {
            throw new AppException("Refresh token has been revoked", HttpStatus.UNAUTHORIZED);
        }

        // RTR Breach Detection: If token was already used, an attacker is replaying it!
        if (Boolean.TRUE.equals(existingToken.getIsUsed())) {
            log.warn("Detected reused refresh token for family: {}. Revoking all tokens in family!", existingToken.getFamilyId());
            refreshTokenRepository.revokeFamily(existingToken.getFamilyId());
            throw new AppException("Invalid refresh token (re-use detected). Session terminated.", HttpStatus.UNAUTHORIZED);
        }

        if (existingToken.isExpired()) {
            throw new AppException("Refresh token expired", HttpStatus.UNAUTHORIZED);
        }

        // Mark old token as used
        existingToken.setIsUsed(true);
        refreshTokenRepository.save(existingToken);

        User user = existingToken.getUser();
        Tenant tenant = existingToken.getTenant();

        // Issue new refresh token under the same family
        String newRawRefreshToken = createRefreshToken(user, tenant, existingToken.getFamilyId(), userAgent, ipAddress);

        TenantMembership activeMembership = null;
        List<TenantMembership> activeMemberships = membershipRepository.findAllByUserIdAndStatusWithTenant(
                user.getId(), MembershipStatus.ACTIVE);

        if (tenant != null) {
            activeMembership = activeMemberships.stream()
                    .filter(m -> m.getTenant().getId().equals(tenant.getId()))
                    .findFirst()
                    .orElse(null);
        }

        AuthResponse authResponse = buildAuthResponse(user, activeMembership, activeMemberships);
        return AuthResult.builder()
                .authResponse(authResponse)
                .rawRefreshToken(newRawRefreshToken)
                .build();
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken, UUID currentUserId) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = TokenHashUtils.hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHashWithUserAndTenant(tokenHash)
                    .ifPresent(token -> {
                        token.setIsRevoked(true);
                        refreshTokenRepository.save(token);
                        refreshTokenRepository.revokeFamily(token.getFamilyId());
                    });
        }
        if (currentUserId != null) {
            refreshTokenRepository.revokeAllByUserId(currentUserId);
        }
    }

    @Override
    @Transactional
    public AuthResult switchTenant(UUID userId, UUID targetTenantId, String userAgent, String ipAddress) {
        User user = userRepository.findById(userId)
                .filter(u -> !u.getIsDeleted())
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        TenantMembership targetMembership = membershipRepository.findByUserIdAndTenantIdWithTenant(userId, targetTenantId)
                .filter(m -> !m.getIsDeleted() && m.getStatus() == MembershipStatus.ACTIVE)
                .orElseThrow(() -> new AppException("You do not have active membership in this company", HttpStatus.FORBIDDEN));

        Tenant tenant = targetMembership.getTenant();
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new AppException("Company is currently " + tenant.getStatus().name().toLowerCase(), HttpStatus.FORBIDDEN);
        }

        List<TenantMembership> activeMemberships = membershipRepository.findAllByUserIdAndStatusWithTenant(
                userId, MembershipStatus.ACTIVE);

        UUID familyId = UUID.randomUUID();
        String rawRefreshToken = createRefreshToken(user, tenant, familyId, userAgent, ipAddress);

        AuthResponse authResponse = buildAuthResponse(user, targetMembership, activeMemberships);
        return AuthResult.builder()
                .authResponse(authResponse)
                .rawRefreshToken(rawRefreshToken)
                .build();
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByEmailIgnoreCaseAndIsDeletedFalse(request.getEmail().trim().toLowerCase());
        if (userOpt.isEmpty()) {
            // For security, do not disclose whether user exists
            return;
        }

        User user = userOpt.get();
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = TokenHashUtils.hashToken(rawToken);

        AuthActionToken actionToken = AuthActionToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .tokenType(ActionTokenType.PASSWORD_RESET)
                .expiresAt(Instant.now().plusSeconds(3600)) // 1 hour validity
                .build();

        actionTokenRepository.save(actionToken);
        log.info("Created password reset token for user '{}': {}", user.getId(), rawToken);
        // Note: Email sending integration will dispatch rawToken in Phase 04 / Notification service
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = TokenHashUtils.hashToken(request.getToken());
        AuthActionToken actionToken = actionTokenRepository.findByTokenHashAndTokenTypeWithUser(
                        tokenHash, ActionTokenType.PASSWORD_RESET)
                .orElseThrow(() -> new AppException("Invalid or expired password reset token", HttpStatus.BAD_REQUEST));

        if (!actionToken.isValid()) {
            throw new AppException("Password reset token is expired or has already been used", HttpStatus.BAD_REQUEST);
        }

        User user = actionToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.resetFailedAttempts();
        userRepository.save(user);

        actionToken.setIsUsed(true);
        actionTokenRepository.save(actionToken);

        // Revoke all existing sessions for security
        refreshTokenRepository.revokeAllByUserId(user.getId());
        log.info("Password successfully reset for user '{}'", user.getId());
    }

    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        String tokenHash = TokenHashUtils.hashToken(request.getToken());
        AuthActionToken actionToken = actionTokenRepository.findByTokenHashAndTokenTypeWithUser(
                        tokenHash, ActionTokenType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new AppException("Invalid or expired verification token", HttpStatus.BAD_REQUEST));

        if (!actionToken.isValid()) {
            throw new AppException("Verification token is expired or has already been used", HttpStatus.BAD_REQUEST);
        }

        User user = actionToken.getUser();
        user.setIsEmailVerified(true);
        userRepository.save(user);

        actionToken.setIsUsed(true);
        actionTokenRepository.save(actionToken);
        log.info("Email verified for user '{}'", user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .filter(u -> !u.getIsDeleted())
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        return authMapper.toUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantMembershipResponse> getUserTenants(UUID userId) {
        List<TenantMembership> memberships = membershipRepository.findAllByUserIdAndStatusWithTenant(
                userId, MembershipStatus.ACTIVE);
        return authMapper.toMembershipResponseList(memberships);
    }

    private String createRefreshToken(User user, Tenant tenant, UUID familyId, String userAgent, String ipAddress) {
        String rawToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        String tokenHash = TokenHashUtils.hashToken(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tenant(tenant)
                .familyId(familyId)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(jwtProperties.getRefreshTokenExpiration()))
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .isRevoked(false)
                .isUsed(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private AuthResponse buildAuthResponse(User user, TenantMembership activeMembership, List<TenantMembership> allMemberships) {
        String accessToken;
        TokenType tokenType;

        if (activeMembership != null && activeMembership.getTenant() != null) {
            tokenType = TokenType.TENANT_SCOPED;
            accessToken = jwtTokenProvider.generateTenantScopedToken(
                    user.getId(),
                    user.getEmail(),
                    activeMembership.getTenant().getId(),
                    activeMembership.getTenant().getSlug(),
                    activeMembership.getRole()
            );
        } else {
            tokenType = TokenType.ACCOUNT_SCOPED;
            accessToken = jwtTokenProvider.generateAccountScopedToken(user.getId(), user.getEmail());
        }

        List<TenantMembership> nonNullMemberships = allMemberships != null
                ? allMemberships.stream().filter(java.util.Objects::nonNull).toList()
                : List.of();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType(tokenType)
                .expiresIn(jwtProperties.getAccessTokenExpiration().toSeconds())
                .user(authMapper.toUserResponse(user))
                .activeTenant(activeMembership != null ? authMapper.toMembershipResponse(activeMembership) : null)
                .availableTenants(authMapper.toMembershipResponseList(nonNullMemberships))
                .build();
    }

    private String generateSlugFromName(String name) {
        return name.toLowerCase().trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
