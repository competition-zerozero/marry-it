package com.zerozero.marryit.workspace.service;

import com.zerozero.marryit.workspace.repository.WorkspaceMemberRepository;
import java.util.Comparator;
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
                .sorted(Comparator
                        .comparing((com.zerozero.marryit.workspace.domain.WorkspaceMember member) -> member.getRole().priority())
                        .thenComparing(com.zerozero.marryit.workspace.domain.WorkspaceMember::getJoinedAt, Comparator.reverseOrder()))
                .map(WorkspaceMemberResponse::from)
                .toList();
    }

    @Transactional
    public WorkspaceMemberResponse updateRole(Long workspaceId, Long actorUserId, Long targetUserId, WorkspaceMemberRoleRequest request) {
        var actorRole = workspaceAccessService.getRole(actorUserId, workspaceId);
        if (!actorRole.canInvite()) {
            throw new SecurityException("Only workspace owners or admins can manage members.");
        }

        var targetMember = workspaceMemberRepository.findByUserIdAndWorkspaceId(targetUserId, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        if (targetMember.getRole() == com.zerozero.marryit.workspace.domain.WorkspaceRole.OWNER) {
            throw new SecurityException("Owner role cannot be changed.");
        }
        if (!actorRole.canGrant(request.role())) {
            throw new SecurityException("You cannot grant this role.");
        }
        if (targetUserId.equals(actorUserId)) {
            throw new SecurityException("You cannot change your own role here.");
        }
        if (actorRole == com.zerozero.marryit.workspace.domain.WorkspaceRole.ADMIN && targetMember.getRole() != com.zerozero.marryit.workspace.domain.WorkspaceRole.MEMBER) {
            throw new SecurityException("Admin can only manage members.");
        }

        targetMember.updateRole(request.role());
        return WorkspaceMemberResponse.from(targetMember);
    }

    @Transactional
    public void removeMember(Long workspaceId, Long actorUserId, Long targetUserId) {
        var actorRole = workspaceAccessService.getRole(actorUserId, workspaceId);
        if (!actorRole.canInvite()) {
            throw new SecurityException("Only workspace owners or admins can manage members.");
        }
        if (targetUserId.equals(actorUserId)) {
            throw new SecurityException("You cannot remove yourself here.");
        }

        var targetMember = workspaceMemberRepository.findByUserIdAndWorkspaceId(targetUserId, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        if (targetMember.getRole() == com.zerozero.marryit.workspace.domain.WorkspaceRole.OWNER) {
            throw new SecurityException("Owner cannot be removed.");
        }
        if (actorRole == com.zerozero.marryit.workspace.domain.WorkspaceRole.ADMIN && targetMember.getRole() != com.zerozero.marryit.workspace.domain.WorkspaceRole.MEMBER) {
            throw new SecurityException("Admin can only remove members.");
        }

        workspaceMemberRepository.delete(targetMember);
    }
}
