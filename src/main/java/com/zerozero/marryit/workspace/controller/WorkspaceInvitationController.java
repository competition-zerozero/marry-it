package com.zerozero.marryit.workspace.controller;

import com.zerozero.marryit.auth.oauth.OAuth2LoginSuccessHandler;
import com.zerozero.marryit.workspace.invitation.service.WorkspaceInvitationRequest;
import com.zerozero.marryit.workspace.invitation.service.WorkspaceInvitationResponse;
import com.zerozero.marryit.workspace.invitation.service.WorkspaceInvitationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class WorkspaceInvitationController {

    private final WorkspaceInvitationService workspaceInvitationService;

    public WorkspaceInvitationController(WorkspaceInvitationService workspaceInvitationService) {
        this.workspaceInvitationService = workspaceInvitationService;
    }

    @PostMapping("/workspaces/{workspaceId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceInvitationResponse invite(
            @PathVariable Long workspaceId,
            @Valid @RequestBody WorkspaceInvitationRequest request,
            HttpSession session
    ) {
        return workspaceInvitationService.invite(workspaceId, currentUserId(session), request);
    }

    @GetMapping("/workspaces/{workspaceId}/invitations")
    public List<WorkspaceInvitationResponse> findAll(@PathVariable Long workspaceId, HttpSession session) {
        return workspaceInvitationService.findAll(workspaceId, currentUserId(session));
    }

    @GetMapping("/workspaces/invitations/{token}")
    public WorkspaceInvitationResponse getByToken(@PathVariable String token) {
        return workspaceInvitationService.getByToken(token);
    }

    @PostMapping("/workspaces/invitations/{token}/accept")
    public WorkspaceInvitationResponse accept(@PathVariable String token, HttpSession session) {
        WorkspaceInvitationResponse response = workspaceInvitationService.accept(token, currentUserId(session));
        session.setAttribute(OAuth2LoginSuccessHandler.SESSION_WORKSPACE_ID, response.workspaceId());
        return response;
    }

    private Long currentUserId(HttpSession session) {
        Object userId = session.getAttribute(OAuth2LoginSuccessHandler.SESSION_USER_ID);
        if (!(userId instanceof Long id)) {
            throw new SecurityException("Login is required.");
        }
        return id;
    }
}
