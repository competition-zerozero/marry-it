package com.zerozero.marryit.workspace.service;

import com.zerozero.marryit.auth.repository.UserRepository;
import com.zerozero.marryit.workspace.domain.Workspace;
import com.zerozero.marryit.workspace.domain.WorkspaceMember;
import com.zerozero.marryit.workspace.repository.WorkspaceMemberRepository;
import com.zerozero.marryit.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateWorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    public CreateWorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            UserRepository userRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public WorkspaceSummaryResponse create(Long userId, CreateWorkspaceRequest request) {
        String name = (request.name() == null || request.name().isBlank())
                ? "새 워크스페이스"
                : request.name().trim();

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        var workspace = workspaceRepository.save(Workspace.create(name));
        var member = workspaceMemberRepository.save(WorkspaceMember.owner(user, workspace));
        return WorkspaceSummaryResponse.from(member);
    }
}
