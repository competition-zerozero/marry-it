package com.zerozero.marryit.workspace.service;

import java.util.List;

public record MeResponse(
        Long userId,
        String email,
        Long currentWorkspaceId,
        List<WorkspaceSummaryResponse> workspaces
) {
}
