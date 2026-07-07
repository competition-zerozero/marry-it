package com.zerozero.marryit.auth.service;

import com.zerozero.marryit.auth.domain.User;
import com.zerozero.marryit.workspace.domain.Workspace;

public record OAuthLoginResult(
        User user,
        Workspace defaultWorkspace,
        boolean firstLogin
) {
}
