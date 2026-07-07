package com.zerozero.marryit.workspace.controller;

import com.zerozero.marryit.auth.oauth.OAuth2LoginSuccessHandler;
import com.zerozero.marryit.workspace.service.MeResponse;
import com.zerozero.marryit.workspace.service.WorkspaceQueryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class WorkspaceController {

    private final WorkspaceQueryService workspaceQueryService;

    public WorkspaceController(WorkspaceQueryService workspaceQueryService) {
        this.workspaceQueryService = workspaceQueryService;
    }

    @GetMapping("/me")
    public MeResponse me(HttpSession session) {
        return workspaceQueryService.getMe(currentUserId(session), currentWorkspaceId(session));
    }

    private Long currentUserId(HttpSession session) {
        Object userId = session.getAttribute(OAuth2LoginSuccessHandler.SESSION_USER_ID);
        if (!(userId instanceof Long id)) {
            throw new SecurityException("Login is required.");
        }
        return id;
    }

    private Long currentWorkspaceId(HttpSession session) {
        Object workspaceId = session.getAttribute(OAuth2LoginSuccessHandler.SESSION_WORKSPACE_ID);
        return workspaceId instanceof Long id ? id : null;
    }
}
