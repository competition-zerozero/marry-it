package com.zerozero.marryit.agent.controller;

import com.zerozero.marryit.agent.service.AgentRequest;
import com.zerozero.marryit.agent.service.AgentResponse;
import com.zerozero.marryit.agent.service.AgentService;
import com.zerozero.marryit.auth.oauth.OAuth2LoginSuccessHandler;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping
    public AgentResponse respond(
            @PathVariable Long workspaceId,
            @Valid @RequestBody AgentRequest request,
            HttpSession session
    ) {
        return agentService.respond(workspaceId, currentUserId(session), request);
    }

    private Long currentUserId(HttpSession session) {
        Object userId = session.getAttribute(OAuth2LoginSuccessHandler.SESSION_USER_ID);
        if (!(userId instanceof Long id)) {
            throw new SecurityException("Login is required.");
        }
        return id;
    }
}
