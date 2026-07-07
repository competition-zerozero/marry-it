package com.zerozero.marryit.workspace.invitation.repository;

import com.zerozero.marryit.workspace.invitation.domain.WorkspaceInvitation;
import com.zerozero.marryit.workspace.invitation.domain.WorkspaceInvitationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, Long> {

    Optional<WorkspaceInvitation> findByToken(String token);

    List<WorkspaceInvitation> findByWorkspaceIdOrderByIdDesc(Long workspaceId);

    boolean existsByWorkspaceIdAndInvitedEmailAndStatus(Long workspaceId, String invitedEmail, WorkspaceInvitationStatus status);
}
