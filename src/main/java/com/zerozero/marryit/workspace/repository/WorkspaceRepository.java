package com.zerozero.marryit.workspace.repository;

import com.zerozero.marryit.workspace.domain.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
}
