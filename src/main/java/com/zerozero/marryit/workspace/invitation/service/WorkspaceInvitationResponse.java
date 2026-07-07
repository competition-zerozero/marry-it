package com.zerozero.marryit.workspace.invitation.service;

import com.zerozero.marryit.workspace.domain.WorkspaceRole;
import com.zerozero.marryit.workspace.invitation.domain.WorkspaceInvitation;
import com.zerozero.marryit.workspace.invitation.domain.WorkspaceInvitationStatus;
import java.time.LocalDateTime;

public record WorkspaceInvitationResponse(
        Long id,
        Long workspaceId,
        String workspaceName,
        String invitedEmail,
        WorkspaceRole role,
        WorkspaceInvitationStatus status,
        LocalDateTime expiresAt,
        String inviteUrl,
        Long invitedByUserId,
        String invitedByName
) {

    public static WorkspaceInvitationResponse from(WorkspaceInvitation invitation) {
        return new WorkspaceInvitationResponse(
                invitation.getId(),
                invitation.getWorkspace().getId(),
                invitation.getWorkspace().getName(),
                invitation.getInvitedEmail(),
                invitation.getRole(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                "/?inviteToken=" + invitation.getToken(),
                invitation.getInvitedBy().getId(),
                invitation.getInvitedBy().getName()
        );
    }
}
