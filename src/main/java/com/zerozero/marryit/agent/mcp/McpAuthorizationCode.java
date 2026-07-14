package com.zerozero.marryit.agent.mcp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class McpAuthorizationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String code;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long workspaceId;

    @Column(nullable = false, length = 1000)
    private String redirectUri;

    @Column(length = 500)
    private String codeChallenge;

    @Column(length = 30)
    private String codeChallengeMethod;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used;

    protected McpAuthorizationCode() {
    }

    private McpAuthorizationCode(
            String code,
            Long userId,
            Long workspaceId,
            String redirectUri,
            String codeChallenge,
            String codeChallengeMethod,
            LocalDateTime expiresAt
    ) {
        this.code = code;
        this.userId = userId;
        this.workspaceId = workspaceId;
        this.redirectUri = redirectUri;
        this.codeChallenge = codeChallenge;
        this.codeChallengeMethod = codeChallengeMethod;
        this.expiresAt = expiresAt;
        this.used = false;
    }

    static McpAuthorizationCode create(
            String code,
            Long userId,
            Long workspaceId,
            String redirectUri,
            String codeChallenge,
            String codeChallengeMethod,
            LocalDateTime expiresAt
    ) {
        return new McpAuthorizationCode(code, userId, workspaceId, redirectUri, codeChallenge, codeChallengeMethod, expiresAt);
    }

    public String getCode() {
        return code;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public String getCodeChallengeMethod() {
        return codeChallengeMethod;
    }

    public boolean isValid(LocalDateTime now) {
        return !used && expiresAt.isAfter(now);
    }

    public void markUsed() {
        this.used = true;
    }
}
