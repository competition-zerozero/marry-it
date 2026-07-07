package com.zerozero.marryit.workspace.service;

import com.zerozero.marryit.workspace.domain.WorkspaceMember;
import com.zerozero.marryit.workspace.domain.WorkspaceRole;

public record WorkspaceSummaryResponse(
        Long workspaceId,
        String workspaceName,
        WorkspaceRole role
) {

    public static WorkspaceSummaryResponse from(WorkspaceMember member) {
        return new WorkspaceSummaryResponse(
                member.getWorkspace().getId(),
                member.getWorkspace().getName(),
                member.getRole()
        );
    }
}
