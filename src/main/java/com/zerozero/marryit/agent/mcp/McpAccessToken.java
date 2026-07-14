package com.zerozero.marryit.agent.mcp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class McpAccessToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 160)
    private String token;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long workspaceId;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    protected McpAccessToken() {
    }

    private McpAccessToken(String token, Long userId, Long workspaceId, LocalDateTime expiresAt) {
        this.token = token;
        this.userId = userId;
        this.workspaceId = workspaceId;
        this.expiresAt = expiresAt;
    }

    static McpAccessToken create(String token, Long userId, Long workspaceId, LocalDateTime expiresAt) {
        return new McpAccessToken(token, userId, workspaceId, expiresAt);
    }

    public Long getUserId() {
        return userId;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public boolean isValid(LocalDateTime now) {
        return expiresAt.isAfter(now);
    }
}
