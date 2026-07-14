package com.zerozero.marryit.agent.mcp;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Profile("mcp")
public class McpAuthenticationFilter extends OncePerRequestFilter {

    private final String apiKey;

    public McpAuthenticationFilter(@Value("${mcp.api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!requiresMcpAuthentication(request) || apiKey == null || apiKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String headerKey = request.getHeader("X-MCP-API-Key");
        String bearerToken = bearerToken(request.getHeader("Authorization"));
        if (apiKey.equals(headerKey) || apiKey.equals(bearerToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized MCP request\"}");
    }

    private boolean requiresMcpAuthentication(HttpServletRequest request) {
        return "/mcp".equals(request.getRequestURI());
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length());
    }
}
