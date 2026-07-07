package com.zerozero.marryit.workspace.domain;

import com.zerozero.marryit.auth.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_workspace_member_workspace_user",
                        columnNames = {"workspace_id", "user_id"}
                )
        }
)
public class WorkspaceMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkspaceRole role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    protected WorkspaceMember() {
    }

    private WorkspaceMember(User user, Workspace workspace, WorkspaceRole role) {
        this.user = user;
        this.workspace = workspace;
        this.role = role;
    }

    public static WorkspaceMember owner(User user, Workspace workspace) {
        return new WorkspaceMember(user, workspace, WorkspaceRole.OWNER);
    }

    public static WorkspaceMember admin(User user, Workspace workspace) {
        return new WorkspaceMember(user, workspace, WorkspaceRole.ADMIN);
    }

    public static WorkspaceMember member(User user, Workspace workspace, WorkspaceRole role) {
        return new WorkspaceMember(user, workspace, role);
    }

    public void updateRole(WorkspaceRole role) {
        this.role = role;
    }

    @PrePersist
    void prePersist() {
        this.joinedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public WorkspaceRole getRole() {
        return role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}
