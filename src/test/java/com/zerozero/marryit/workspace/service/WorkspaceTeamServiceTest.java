package com.zerozero.marryit.workspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zerozero.marryit.auth.domain.OAuthProvider;
import com.zerozero.marryit.auth.domain.User;
import com.zerozero.marryit.auth.repository.UserRepository;
import com.zerozero.marryit.workspace.domain.Workspace;
import com.zerozero.marryit.workspace.domain.WorkspaceMember;
import com.zerozero.marryit.workspace.domain.WorkspaceRole;
import com.zerozero.marryit.workspace.repository.WorkspaceMemberRepository;
import com.zerozero.marryit.workspace.repository.WorkspaceRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({WorkspaceTeamService.class, WorkspaceAccessService.class})
class WorkspaceTeamServiceTest {

    @Autowired
    private WorkspaceTeamService workspaceTeamService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Test
    void ownerCanPromoteMemberAndRemoveMember() {
        User owner = saveUser("google-owner", "owner@example.com", "오너");
        User member = saveUser("google-member", "member@example.com", "멤버");
        Workspace workspace = workspaceRepository.save(Workspace.createPersonal(owner.getName()));
        workspaceMemberRepository.saveAll(List.of(
                WorkspaceMember.owner(owner, workspace),
                WorkspaceMember.member(member, workspace, WorkspaceRole.MEMBER)
        ));

        WorkspaceMemberResponse updated = workspaceTeamService.updateRole(
                workspace.getId(),
                owner.getId(),
                member.getId(),
                new WorkspaceMemberRoleRequest(WorkspaceRole.ADMIN)
        );
        workspaceTeamService.removeMember(workspace.getId(), owner.getId(), member.getId());

        assertThat(updated.role()).isEqualTo(WorkspaceRole.ADMIN);
        assertThat(workspaceMemberRepository.existsByUserIdAndWorkspaceId(member.getId(), workspace.getId())).isFalse();
    }

    @Test
    void adminCanOnlyManageMembers() {
        User owner = saveUser("google-owner", "owner@example.com", "오너");
        User admin = saveUser("google-admin", "admin@example.com", "관리자");
        User member = saveUser("google-member", "member@example.com", "멤버");
        Workspace workspace = workspaceRepository.save(Workspace.createPersonal(owner.getName()));
        workspaceMemberRepository.saveAll(List.of(
                WorkspaceMember.owner(owner, workspace),
                WorkspaceMember.admin(admin, workspace),
                WorkspaceMember.member(member, workspace, WorkspaceRole.MEMBER)
        ));

        WorkspaceMemberResponse updated = workspaceTeamService.updateRole(
                workspace.getId(),
                admin.getId(),
                member.getId(),
                new WorkspaceMemberRoleRequest(WorkspaceRole.MEMBER)
        );

        assertThat(updated.role()).isEqualTo(WorkspaceRole.MEMBER);
        assertThatThrownBy(() -> workspaceTeamService.updateRole(
                workspace.getId(),
                admin.getId(),
                owner.getId(),
                new WorkspaceMemberRoleRequest(WorkspaceRole.MEMBER)
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
