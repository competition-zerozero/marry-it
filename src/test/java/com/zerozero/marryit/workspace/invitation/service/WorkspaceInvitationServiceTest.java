package com.zerozero.marryit.workspace.invitation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zerozero.marryit.auth.domain.OAuthProvider;
import com.zerozero.marryit.auth.domain.User;
import com.zerozero.marryit.auth.repository.UserRepository;
import com.zerozero.marryit.workspace.domain.Workspace;
import com.zerozero.marryit.workspace.domain.WorkspaceMember;
import com.zerozero.marryit.workspace.domain.WorkspaceRole;
import com.zerozero.marryit.workspace.invitation.repository.WorkspaceInvitationRepository;
import com.zerozero.marryit.workspace.repository.WorkspaceMemberRepository;
import com.zerozero.marryit.workspace.repository.WorkspaceRepository;
import com.zerozero.marryit.workspace.service.WorkspaceAccessService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({WorkspaceInvitationService.class, WorkspaceInvitationMailService.class, WorkspaceAccessService.class})
class WorkspaceInvitationServiceTest {

    @Autowired
    private WorkspaceInvitationService workspaceInvitationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private WorkspaceInvitationRepository workspaceInvitationRepository;

    @Test
    void ownerCanInviteMemberToWorkspace() {
        User owner = saveUser("google-owner", "owner@example.com", "오너");
        Workspace workspace = workspaceRepository.save(Workspace.createPersonal(owner.getName()));
        workspaceMemberRepository.save(WorkspaceMember.owner(owner, workspace));

        WorkspaceInvitationResponse response = workspaceInvitationService.invite(
                workspace.getId(),
                owner.getId(),
                new WorkspaceInvitationRequest("planner@example.com", WorkspaceRole.MEMBER)
        );

        assertThat(response.workspaceId()).isEqualTo(workspace.getId());
        assertThat(response.invitedEmail()).isEqualTo("planner@example.com");
        assertThat(response.role()).isEqualTo(WorkspaceRole.MEMBER);
        assertThat(response.inviteUrl()).contains("inviteToken=");
        assertThat(workspaceInvitationRepository.count()).isEqualTo(1);
    }

    @Test
    void adminCanOnlyInviteMemberRole() {
        User owner = saveUser("google-owner", "owner@example.com", "오너");
        User admin = saveUser("google-admin", "admin@example.com", "관리자");
        Workspace workspace = workspaceRepository.save(Workspace.createPersonal(owner.getName()));
        workspaceMemberRepository.saveAll(List.of(
                WorkspaceMember.owner(owner, workspace),
                WorkspaceMember.admin(admin, workspace)
        ));

        WorkspaceInvitationResponse response = workspaceInvitationService.invite(
                workspace.getId(),
                admin.getId(),
                new WorkspaceInvitationRequest("planner@example.com", WorkspaceRole.MEMBER)
        );

        assertThat(response.role()).isEqualTo(WorkspaceRole.MEMBER);

        assertThatThrownBy(() -> workspaceInvitationService.invite(
                workspace.getId(),
                admin.getId(),
                new WorkspaceInvitationRequest("planner2@example.com", WorkspaceRole.ADMIN)
        )).isInstanceOf(SecurityException.class);
    }

    @Test
    void acceptInvitationCreatesMembershipWhenEmailMatches() {
        User owner = saveUser("google-owner", "owner@example.com", "오너");
        User planner = saveUser("google-planner", "planner@example.com", "플래너");
        Workspace workspace = workspaceRepository.save(Workspace.createPersonal(owner.getName()));
        workspaceMemberRepository.save(WorkspaceMember.owner(owner, workspace));
        WorkspaceInvitationResponse invitation = workspaceInvitationService.invite(
                workspace.getId(),
                owner.getId(),
                new WorkspaceInvitationRequest("planner@example.com", WorkspaceRole.ADMIN)
        );

        WorkspaceInvitationResponse accepted = workspaceInvitationService.accept(invitation.inviteUrl().substring("/?inviteToken=".length()), planner.getId());

        assertThat(accepted.status()).isEqualTo(com.zerozero.marryit.workspace.invitation.domain.WorkspaceInvitationStatus.ACCEPTED);
        assertThat(workspaceMemberRepository.existsByUserIdAndWorkspaceId(planner.getId(), workspace.getId())).isTrue();
    }

    @Test
    void rejectsInvitationAcceptanceForDifferentEmail() {
        User owner = saveUser("google-owner", "owner@example.com", "오너");
        User other = saveUser("google-other", "other@example.com", "다른사람");
        Workspace workspace = workspaceRepository.save(Workspace.createPersonal(owner.getName()));
        workspaceMemberRepository.save(WorkspaceMember.owner(owner, workspace));
        WorkspaceInvitationResponse invitation = workspaceInvitationService.invite(
                workspace.getId(),
                owner.getId(),
                new WorkspaceInvitationRequest("planner@example.com", WorkspaceRole.MEMBER)
        );

        assertThatThrownBy(() -> workspaceInvitationService.accept(
                invitation.inviteUrl().substring("/?inviteToken=".length()),
                other.getId()
        )).isInstanceOf(SecurityException.class);
    }

    private User saveUser(String providerUserId, String email, String name) {
        return userRepository.save(User.createOAuthUser(
                OAuthProvider.GOOGLE,
                providerUserId,
                email,
                name,
                null
        ));
    }

}
