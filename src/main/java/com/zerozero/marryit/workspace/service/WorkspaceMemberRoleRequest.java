package com.zerozero.marryit.workspace.service;

import com.zerozero.marryit.workspace.domain.WorkspaceRole;
import jakarta.validation.constraints.NotNull;

public record WorkspaceMemberRoleRequest(
        @NotNull
        WorkspaceRole role
) {
}
