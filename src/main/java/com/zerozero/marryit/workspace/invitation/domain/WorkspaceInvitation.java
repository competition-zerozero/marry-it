package com.zerozero.marryit.workspace.invitation.domain;

import com.zerozero.marryit.auth.domain.User;
import com.zerozero.marryit.global.entity.BaseTimeEntity;
import com.zerozero.marryit.workspace.domain.Workspace;
import com.zerozero.marryit.workspace.domain.WorkspaceRole;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_workspace_invitation_token",
                        columnNames = {"token"}
                )
        }
)
public class WorkspaceInvitation extends BaseTimeEntity {

    private static final int DEFAULT_EXPIRATION_DAYS = 7;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by_user_id", nullable = false)
    private User invitedBy;

    @Column(nullable = false, length = 255)
    private String invitedEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkspaceRole role;

    @Column(nullable = false, length = 80)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkspaceInvitationStatus status;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime acceptedAt;

    protected WorkspaceInvitation() {
    }

    private WorkspaceInvitation(Workspace workspace, User invitedBy, String invitedEmail, WorkspaceRole role) {
        this.workspace = workspace;
        this.invitedBy = invitedBy;
        this.invitedEmail = invitedEmail;
        this.role = role;
        this.token = UUID.randomUUID().toString().replace("-", "");
        this.status = WorkspaceInvitationStatus.PENDING;
        this.expiresAt = LocalDateTime.now().plusDays(DEFAULT_EXPIRATION_DAYS);
    }

    public static WorkspaceInvitation create(
            Workspace workspace,
            User invitedBy,
            String invitedEmail,
            WorkspaceRole role
    ) {
        return new WorkspaceInvitation(workspace, invitedBy, invitedEmail, role);
    }

    public boolean isPending() {
        return status == WorkspaceInvitationStatus.PENDING;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void accept() {
        this.status = WorkspaceInvitationStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public User getInvitedBy() {
        return invitedBy;
    }

    public String getInvitedEmail() {
        return invitedEmail;
    }

    public WorkspaceRole getRole() {
        return role;
    }

    public String getToken() {
        return token;
    }

    public WorkspaceInvitationStatus getStatus() {
        return status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }
}
