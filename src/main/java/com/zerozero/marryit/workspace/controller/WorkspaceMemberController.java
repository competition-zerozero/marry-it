package com.zerozero.marryit.workspace.controller;

import com.zerozero.marryit.auth.oauth.OAuth2LoginSuccessHandler;
import com.zerozero.marryit.workspace.service.WorkspaceMemberRoleRequest;
import com.zerozero.marryit.workspace.service.WorkspaceMemberResponse;
import com.zerozero.marryit.workspace.service.WorkspaceTeamService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/members")
public class WorkspaceMemberController {

    private final WorkspaceTeamService workspaceTeamService;

    public WorkspaceMemberController(WorkspaceTeamService workspaceTeamService) {
        this.workspaceTeamService = workspaceTeamService;
    }

    @GetMapping
    public List<WorkspaceMemberResponse> findAll(@PathVariable Long workspaceId, HttpSession session) {
        return workspaceTeamService.findMembers(workspaceId, currentUserId(session));
    }

    @PatchMapping("/{targetUserId}/role")
    public WorkspaceMemberResponse updateRole(
            @PathVariable Long workspaceId,
            @PathVariable Long targetUserId,
            @Valid @RequestBody WorkspaceMemberRoleRequest request,
            HttpSession session
    ) {
        return workspaceTeamService.updateRole(workspaceId, currentUserId(session), targetUserId, request);
    }

    @DeleteMapping("/{targetUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable Long workspaceId,
            @PathVariable Long targetUserId,
            HttpSession session
    ) {
        workspaceTeamService.removeMember(workspaceId, currentUserId(session), targetUserId);
    }

    private Long currentUserId(HttpSession session) {
        Object userId = session.getAttribute(OAuth2LoginSuccessHandler.SESSION_USER_ID);
        if (!(userId instanceof Long id)) {
            throw new SecurityException("Login is required.");
        }
        return id;
    }
}
