package com.zerozero.marryit.workspace.invitation.service;

import com.zerozero.marryit.auth.domain.User;
import com.zerozero.marryit.auth.repository.UserRepository;
import com.zerozero.marryit.workspace.domain.Workspace;
import com.zerozero.marryit.workspace.domain.WorkspaceMember;
import com.zerozero.marryit.workspace.domain.WorkspaceRole;
import com.zerozero.marryit.workspace.invitation.domain.WorkspaceInvitation;
import com.zerozero.marryit.workspace.invitation.domain.WorkspaceInvitationStatus;
import com.zerozero.marryit.workspace.invitation.repository.WorkspaceInvitationRepository;
import com.zerozero.marryit.workspace.repository.WorkspaceMemberRepository;
import com.zerozero.marryit.workspace.repository.WorkspaceRepository;
import com.zerozero.marryit.workspace.service.WorkspaceAccessService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceInvitationService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final WorkspaceAccessService workspaceAccessService;
    private final UserRepository userRepository;
    private final WorkspaceInvitationMailService workspaceInvitationMailService;

    public WorkspaceInvitationService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            WorkspaceInvitationRepository workspaceInvitationRepository,
            WorkspaceAccessService workspaceAccessService,
            UserRepository userRepository,
            WorkspaceInvitationMailService workspaceInvitationMailService
    ) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.workspaceInvitationRepository = workspaceInvitationRepository;
        this.workspaceAccessService = workspaceAccessService;
        this.userRepository = userRepository;
        this.workspaceInvitationMailService = workspaceInvitationMailService;
    }

    @Transactional
    public WorkspaceInvitationResponse invite(Long workspaceId, Long inviterUserId, WorkspaceInvitationRequest request) {
        WorkspaceRole inviterRole = workspaceAccessService.getRole(inviterUserId, workspaceId);
        if (!inviterRole.canInvite()) {
            throw new SecurityException("Only workspace owners or admins can invite members.");
        }
        if (!inviterRole.canGrant(request.role())) {
            throw new SecurityException("You cannot grant this role.");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found."));
        User invitedBy = userRepository.findById(inviterUserId)
                .orElseThrow(() -> new IllegalArgumentException("Inviter not found."));

        workspaceInvitationRepository.findByWorkspaceIdOrderByIdDesc(workspaceId).stream()
                .filter(invitation -> invitation.getInvitedEmail().equalsIgnoreCase(request.invitedEmail()))
                .filter(WorkspaceInvitation::isPending)
                .findFirst()
                .ifPresent(invitation -> {
                    throw new IllegalArgumentException("An active invitation already exists for this email.");
                });

        WorkspaceInvitation invitation = WorkspaceInvitation.create(
                workspace,
                invitedBy,
                request.invitedEmail(),
                request.role()
        );
        WorkspaceInvitationResponse response = WorkspaceInvitationResponse.from(workspaceInvitationRepository.save(invitation));
        workspaceInvitationMailService.sendInvitation(response);
        return response;
    }

    @Transactional(readOnly = true)
    public List<WorkspaceInvitationResponse> findAll(Long workspaceId, Long userId) {
        WorkspaceRole role = workspaceAccessService.getRole(userId, workspaceId);
        if (!role.canInvite()) {
            throw new SecurityException("Only workspace owners or admins can view invitations.");
        }
        return workspaceInvitationRepository.findByWorkspaceIdOrderByIdDesc(workspaceId)
                .stream()
                .map(WorkspaceInvitationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkspaceInvitationResponse> findMyInvitations(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        return workspaceInvitationRepository.findByInvitedEmailIgnoreCaseOrderByIdDesc(user.getEmail())
                .stream()
                .map(WorkspaceInvitationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceInvitationResponse getByToken(String token) {
        return WorkspaceInvitationResponse.from(workspaceInvitationRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found.")));
    }

    @Transactional
    public WorkspaceInvitationResponse decline(String token, Long userId) {
        WorkspaceInvitation invitation = workspaceInvitationRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found."));

        if (!invitation.isPending()) {
            throw new IllegalStateException("Invitation is no longer active.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (!invitation.getInvitedEmail().equalsIgnoreCase(user.getEmail())) {
            throw new SecurityException("This invitation was sent to a different email address.");
        }

        invitation.decline();
        return WorkspaceInvitationResponse.from(invitation);
    }

    @Transactional
    public WorkspaceInvitationResponse accept(String token, Long userId) {
        WorkspaceInvitation invitation = workspaceInvitationRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found."));

        if (!invitation.isPending()) {
            throw new IllegalStateException("Invitation is no longer active.");
        }
        if (invitation.isExpired()) {
            throw new IllegalStateException("Invitation has expired.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (!invitation.getInvitedEmail().equalsIgnoreCase(user.getEmail())) {
            throw new SecurityException("This invitation was sent to a different email address.");
        }

        WorkspaceMember workspaceMember = workspaceMemberRepository.findByUserIdAndWorkspaceId(userId, invitation.getWorkspace().getId())
                .orElse(null);
        if (workspaceMember == null) {
            workspaceMemberRepository.save(WorkspaceMember.member(user, invitation.getWorkspace(), invitation.getRole()));
        } else if (invitation.getRole().isHigherThan(workspaceMember.getRole())) {
            workspaceMember.updateRole(invitation.getRole());
        }

        invitation.accept();
        return WorkspaceInvitationResponse.from(invitation);
    }
}
