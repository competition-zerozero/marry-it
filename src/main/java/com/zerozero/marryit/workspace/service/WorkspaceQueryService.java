package com.zerozero.marryit.workspace.service;

import com.zerozero.marryit.auth.repository.UserRepository;
import com.zerozero.marryit.workspace.repository.WorkspaceMemberRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceQueryService {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    public WorkspaceQueryService(WorkspaceMemberRepository workspaceMemberRepository, UserRepository userRepository) {
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public MeResponse getMe(Long userId, Long currentWorkspaceId) {
        String email = userRepository.findById(userId).map(u -> u.getEmail()).orElse(null);
        List<WorkspaceSummaryResponse> workspaces = workspaceMemberRepository.findByUserId(userId)
                .stream()
                .map(WorkspaceSummaryResponse::from)
                .toList();
        return new MeResponse(userId, email, currentWorkspaceId, workspaces);
    }
}
