package com.zerozero.marryit.workspace.repository;

import com.zerozero.marryit.workspace.domain.WorkspaceMember;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    List<WorkspaceMember> findByUserId(Long userId);

    boolean existsByUserIdAndWorkspaceId(Long userId, Long workspaceId);
}
