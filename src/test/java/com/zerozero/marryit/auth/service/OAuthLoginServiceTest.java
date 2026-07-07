package com.zerozero.marryit.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.zerozero.marryit.auth.domain.OAuthProvider;
import com.zerozero.marryit.auth.repository.UserRepository;
import com.zerozero.marryit.workspace.domain.WorkspaceRole;
import com.zerozero.marryit.workspace.repository.WorkspaceMemberRepository;
import com.zerozero.marryit.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(OAuthLoginService.class)
class OAuthLoginServiceTest {

    @Autowired
    private OAuthLoginService oauthLoginService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Test
    void createsUserPersonalWorkspaceAndOwnerMembershipOnFirstLogin() {
        OAuthLoginResult result = oauthLoginService.login(googleProfile("google-user-1", "planner@example.com", "서영"));

        assertThat(result.firstLogin()).isTrue();
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(workspaceRepository.count()).isEqualTo(1);
        assertThat(workspaceMemberRepository.count()).isEqualTo(1);

        var membership = workspaceMemberRepository.findByUserId(result.user().getId()).getFirst();
        assertThat(membership.getWorkspace().getId()).isEqualTo(result.defaultWorkspace().getId());
        assertThat(membership.getRole()).isEqualTo(WorkspaceRole.OWNER);
    }

    @Test
    void doesNotCreateDuplicateWorkspaceOnRepeatLogin() {
        oauthLoginService.login(googleProfile("google-user-1", "old@example.com", "서영"));

        OAuthLoginResult result = oauthLoginService.login(
                googleProfile("google-user-1", "new@example.com", "서영 플래너")
        );

        assertThat(result.firstLogin()).isFalse();
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(workspaceRepository.count()).isEqualTo(1);
        assertThat(workspaceMemberRepository.count()).isEqualTo(1);
        assertThat(result.user().getEmail()).isEqualTo("new@example.com");
        assertThat(result.user().getName()).isEqualTo("서영 플래너");
    }

    private OAuthUserProfile googleProfile(String providerUserId, String email, String name) {
        return new OAuthUserProfile(
                OAuthProvider.GOOGLE,
                providerUserId,
                email,
                name,
                "https://example.com/profile.png"
        );
    }
}
