package com.zerozero.marryit.workspace.invitation.service;

import com.zerozero.marryit.workspace.domain.WorkspaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkspaceInvitationRequest(
        @Email
        @NotBlank
        String invitedEmail,
        @NotNull
        WorkspaceRole role
) {
}
