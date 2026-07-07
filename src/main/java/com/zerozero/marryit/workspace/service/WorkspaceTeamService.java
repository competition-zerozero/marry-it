package com.zerozero.marryit.workspace.service;

import com.zerozero.marryit.workspace.repository.WorkspaceMemberRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceTeamService {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceAccessService workspaceAccessService;

    public WorkspaceTeamService(
            WorkspaceMemberRepository workspaceMemberRepository,
            WorkspaceAccessService workspaceAccessService
    ) {
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.workspaceAccessService = workspaceAccessService;
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> findMembers(Long workspaceId, Long userId) {
        workspaceAccessService.validateMember(userId, workspaceId);
        return workspaceMemberRepository.findByWorkspaceIdOrderByIdDesc(workspaceId)
                .stream()
                .map(WorkspaceMemberResponse::from)
                .toList();
    }
}
