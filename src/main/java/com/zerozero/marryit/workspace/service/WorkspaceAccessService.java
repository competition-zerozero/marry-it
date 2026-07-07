package com.zerozero.marryit.workspace.service;

import com.zerozero.marryit.workspace.repository.WorkspaceMemberRepository;
import com.zerozero.marryit.workspace.domain.WorkspaceRole;
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

    @Transactional(readOnly = true)
    public WorkspaceRole getRole(Long userId, Long workspaceId) {
        return workspaceMemberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                .map(member -> member.getRole())
                .orElseThrow(() -> new SecurityException("User cannot access this workspace."));
    }
}
