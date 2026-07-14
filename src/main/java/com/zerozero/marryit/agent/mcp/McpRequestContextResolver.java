package com.zerozero.marryit.agent.mcp;

import com.zerozero.marryit.agent.tool.AgentToolContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class McpRequestContextResolver {

    private final McpOAuthService mcpOAuthService;
    private final Long fallbackWorkspaceId;
    private final Long fallbackUserId;

    public McpRequestContextResolver(
            McpOAuthService mcpOAuthService,
            @Value("${mcp.workspace-id:0}") Long fallbackWorkspaceId,
            @Value("${mcp.user-id:0}") Long fallbackUserId
    ) {
        this.mcpOAuthService = mcpOAuthService;
        this.fallbackWorkspaceId = fallbackWorkspaceId;
        this.fallbackUserId = fallbackUserId;
    }

    public AgentToolContext resolve(HttpServletRequest request) {
        String bearerToken = bearerToken(request.getHeader("Authorization"));
        if (bearerToken != null) {
            return mcpOAuthService.resolveContext(bearerToken);
        }
        if (fallbackWorkspaceId != null && fallbackWorkspaceId > 0 && fallbackUserId != null && fallbackUserId > 0) {
            return new AgentToolContext(fallbackWorkspaceId, fallbackUserId);
        }
        throw new SecurityException("MCP OAuth login is required.");
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length());
    }
}
