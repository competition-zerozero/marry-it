package com.zerozero.marryit.auth.service;

import com.zerozero.marryit.auth.domain.User;
import com.zerozero.marryit.auth.repository.UserRepository;
import com.zerozero.marryit.workspace.domain.Workspace;
import com.zerozero.marryit.workspace.domain.WorkspaceMember;
import com.zerozero.marryit.workspace.domain.WorkspaceRole;
import com.zerozero.marryit.workspace.repository.WorkspaceMemberRepository;
import com.zerozero.marryit.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuthLoginService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public OAuthLoginService(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository
    ) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @Transactional
    public OAuthLoginResult login(OAuthUserProfile profile) {
        return userRepository.findByProviderAndProviderUserId(profile.provider(), profile.providerUserId())
                .map(user -> loginExistingUser(user, profile))
                .orElseGet(() -> createUserAndPersonalWorkspace(profile));
    }

    private OAuthLoginResult loginExistingUser(User user, OAuthUserProfile profile) {
        user.updateOAuthProfile(profile.email(), profile.name(), profile.profileImageUrl());

        List<WorkspaceMember> memberships = workspaceMemberRepository.findByUserId(user.getId());
        Workspace defaultWorkspace = memberships.stream()
                .sorted(Comparator
                        .comparingInt((WorkspaceMember member) -> rolePriority(member.getRole()))
                        .thenComparing(WorkspaceMember::getJoinedAt, Comparator.reverseOrder()))
                .findFirst()
                .map(WorkspaceMember::getWorkspace)
                .orElseThrow(() -> new IllegalStateException("User has no workspace."));

        return new OAuthLoginResult(user, defaultWorkspace, false);
    }

    private OAuthLoginResult createUserAndPersonalWorkspace(OAuthUserProfile profile) {
        User user = User.createOAuthUser(
                profile.provider(),
                profile.providerUserId(),
                profile.email(),
                profile.name(),
                profile.profileImageUrl()
        );
        User savedUser = userRepository.save(user);

        Workspace workspace = workspaceRepository.save(Workspace.createPersonal(savedUser.getName()));
        workspaceMemberRepository.save(WorkspaceMember.owner(savedUser, workspace));

        return new OAuthLoginResult(savedUser, workspace, true);
    }

    private int rolePriority(WorkspaceRole role) {
        return switch (role) {
            case OWNER -> 0;
            case ADMIN -> 1;
            case MEMBER -> 2;
        };
    }
}
