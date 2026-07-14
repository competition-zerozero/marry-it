package com.zerozero.marryit.agent.mcp;

import com.zerozero.marryit.agent.tool.AgentToolContext;
import com.zerozero.marryit.workspace.domain.WorkspaceMember;
import com.zerozero.marryit.workspace.repository.WorkspaceMemberRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class McpOAuthService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration TOKEN_TTL = Duration.ofDays(30);

    private final McpAuthorizationCodeRepository authorizationCodeRepository;
    private final McpAccessTokenRepository accessTokenRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public McpOAuthService(
            McpAuthorizationCodeRepository authorizationCodeRepository,
            McpAccessTokenRepository accessTokenRepository,
            WorkspaceMemberRepository workspaceMemberRepository
    ) {
        this.authorizationCodeRepository = authorizationCodeRepository;
        this.accessTokenRepository = accessTokenRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @Transactional
    public String createAuthorizationCode(
            Long userId,
            String redirectUri,
            String codeChallenge,
            String codeChallengeMethod
    ) {
        WorkspaceMember defaultMembership = workspaceMemberRepository.findByUserId(userId).stream()
                .sorted(Comparator
                        .comparingInt((WorkspaceMember member) -> rolePriority(member.getRole()))
                        .thenComparing(WorkspaceMember::getJoinedAt, Comparator.reverseOrder()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User has no workspace."));

        String code = randomToken(32);
        authorizationCodeRepository.save(McpAuthorizationCode.create(
                code,
                userId,
                defaultMembership.getWorkspace().getId(),
                redirectUri,
                codeChallenge,
                codeChallengeMethod,
                LocalDateTime.now().plus(CODE_TTL)
        ));
        return code;
    }

    @Transactional
    public Map<String, Object> exchangeAuthorizationCode(
            String code,
            String redirectUri,
            String codeVerifier
    ) {
        McpAuthorizationCode authorizationCode = authorizationCodeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid authorization code."));
        if (!authorizationCode.isValid(LocalDateTime.now())) {
            throw new IllegalArgumentException("Expired or used authorization code.");
        }
        if (!authorizationCode.getRedirectUri().equals(redirectUri)) {
            throw new IllegalArgumentException("redirect_uri does not match authorization request.");
        }
        if (!matchesPkce(authorizationCode, codeVerifier)) {
            throw new IllegalArgumentException("PKCE verification failed.");
        }

        authorizationCode.markUsed();

        String token = randomToken(48);
        accessTokenRepository.save(McpAccessToken.create(
                token,
                authorizationCode.getUserId(),
                authorizationCode.getWorkspaceId(),
                LocalDateTime.now().plus(TOKEN_TTL)
        ));

        return Map.of(
                "access_token", token,
                "token_type", "Bearer",
                "expires_in", TOKEN_TTL.toSeconds(),
                "scope", "mcp:tools"
        );
    }

    @Transactional(readOnly = true)
    public AgentToolContext resolveContext(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new SecurityException("Missing MCP bearer token.");
        }
        McpAccessToken accessToken = accessTokenRepository.findByToken(bearerToken)
                .orElseThrow(() -> new SecurityException("Invalid MCP bearer token."));
        if (!accessToken.isValid(LocalDateTime.now())) {
            throw new SecurityException("Expired MCP bearer token.");
        }
        return new AgentToolContext(accessToken.getWorkspaceId(), accessToken.getUserId());
    }

    private boolean matchesPkce(McpAuthorizationCode authorizationCode, String codeVerifier) {
        String challenge = authorizationCode.getCodeChallenge();
        if (challenge == null || challenge.isBlank()) {
            return true;
        }
        if (codeVerifier == null || codeVerifier.isBlank()) {
            return false;
        }
        String method = authorizationCode.getCodeChallengeMethod();
        if (method == null || method.isBlank() || "plain".equalsIgnoreCase(method)) {
            return challenge.equals(codeVerifier);
        }
        if ("S256".equalsIgnoreCase(method)) {
            return challenge.equals(sha256Base64Url(codeVerifier));
        }
        return false;
    }

    private String sha256Base64Url(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot verify PKCE code challenge.", exception);
        }
    }

    private String randomToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private int rolePriority(com.zerozero.marryit.workspace.domain.WorkspaceRole role) {
        return switch (role) {
            case OWNER -> 0;
            case ADMIN -> 1;
            case MEMBER -> 2;
        };
    }
}
