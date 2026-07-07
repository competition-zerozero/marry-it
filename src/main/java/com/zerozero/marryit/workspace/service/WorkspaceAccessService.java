package com.zerozero.marryit.workspace.service;

import com.zerozero.marryit.workspace.repository.WorkspaceMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceAccessService {

    private final WorkspaceMemberRepository workspaceMemberRepository;

    public WorkspaceAccessService(WorkspaceMemberRepository workspaceMemberRepository) {
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @Transactional(readOnly = true)
    public void validateMember(Long userId, Long workspaceId) {
        if (!workspaceMemberRepository.existsByUserIdAndWorkspaceId(userId, workspaceId)) {
            throw new SecurityException("User cannot access this workspace.");
        }
    }
}
