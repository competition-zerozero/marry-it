package com.zerozero.marryit.workspace.service;

import com.zerozero.marryit.workspace.repository.WorkspaceMemberRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceQueryService {

    private final WorkspaceMemberRepository workspaceMemberRepository;

    public WorkspaceQueryService(WorkspaceMemberRepository workspaceMemberRepository) {
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @Transactional(readOnly = true)
    public MeResponse getMe(Long userId, Long currentWorkspaceId) {
        List<WorkspaceSummaryResponse> workspaces = workspaceMemberRepository.findByUserId(userId)
                .stream()
                .map(WorkspaceSummaryResponse::from)
                .toList();
        return new MeResponse(userId, currentWorkspaceId, workspaces);
    }
}
