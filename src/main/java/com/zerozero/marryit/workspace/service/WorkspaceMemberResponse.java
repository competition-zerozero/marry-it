package com.zerozero.marryit.workspace.service;

import com.zerozero.marryit.workspace.domain.WorkspaceMember;
import com.zerozero.marryit.workspace.domain.WorkspaceRole;
import java.time.LocalDateTime;

public record WorkspaceMemberResponse(
        Long userId,
        String userName,
        String userEmail,
        WorkspaceRole role,
        LocalDateTime joinedAt
) {

    public static WorkspaceMemberResponse from(WorkspaceMember member) {
        return new WorkspaceMemberResponse(
                member.getUser().getId(),
                member.getUser().getName(),
                member.getUser().getEmail(),
                member.getRole(),
                member.getJoinedAt()
        );
    }
}
