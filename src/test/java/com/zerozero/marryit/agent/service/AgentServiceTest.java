package com.zerozero.marryit.agent.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.zerozero.marryit.auth.domain.OAuthProvider;
import com.zerozero.marryit.auth.domain.User;
import com.zerozero.marryit.auth.repository.UserRepository;
import com.zerozero.marryit.external.kakao.KakaoPlaceClient;
import com.zerozero.marryit.recommendation.service.VendorRecommendationService;
import com.zerozero.marryit.vendor.domain.VendorCategory;
import com.zerozero.marryit.vendor.repository.VendorRepository;
import com.zerozero.marryit.workspace.domain.Workspace;
import com.zerozero.marryit.workspace.domain.WorkspaceMember;
import com.zerozero.marryit.workspace.repository.WorkspaceMemberRepository;
import com.zerozero.marryit.workspace.repository.WorkspaceRepository;
import com.zerozero.marryit.workspace.service.WorkspaceAccessService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({
        AgentService.class,
        VendorRecommendationService.class,
        WorkspaceAccessService.class,
        AgentServiceTest.FakeKakaoConfig.class
})
class AgentServiceTest {

    @Autowired
    private AgentService agentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @SuppressWarnings("unused")
    @Autowired
    private VendorRepository vendorRepository;

    @Test
    void doesNotInventRecommendationWithoutCategory() {
        User planner = saveUser();
        Workspace workspace = saveWorkspaceWithOwner(planner);

        AgentResponse response = agentService.respond(
                workspace.getId(),
                planner.getId(),
                new AgentRequest("추천해줘", null, "강남", true)
        );

        assertThat(response.vendorRecommendation()).isNull();
        assertThat(response.answer()).contains("카테고리");
    }

    private User saveUser() {
        return userRepository.save(User.createOAuthUser(
                OAuthProvider.GOOGLE,
                "google-1",
                "planner@example.com",
                "서영",
                null
        ));
    }

    private Workspace saveWorkspaceWithOwner(User owner) {
        Workspace workspace = workspaceRepository.save(Workspace.createPersonal(owner.getName()));
        workspaceMemberRepository.save(WorkspaceMember.owner(owner, workspace));
        return workspace;
    }

    @TestConfiguration
    static class FakeKakaoConfig {

        @Bean
        KakaoPlaceClient kakaoPlaceClient() {
            return query -> List.of();
        }
    }
}
