package com.zerozero.marryit.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.zerozero.marryit.auth.domain.OAuthProvider;
import com.zerozero.marryit.auth.domain.User;
import com.zerozero.marryit.auth.repository.UserRepository;
import com.zerozero.marryit.external.kakao.KakaoPlaceClient;
import com.zerozero.marryit.external.kakao.KakaoPlaceResponse;
import com.zerozero.marryit.vendor.domain.Vendor;
import com.zerozero.marryit.vendor.domain.VendorCategory;
import com.zerozero.marryit.vendor.repository.VendorRepository;
import com.zerozero.marryit.workspace.domain.Workspace;
import com.zerozero.marryit.workspace.domain.WorkspaceMember;
import com.zerozero.marryit.workspace.repository.WorkspaceMemberRepository;
import com.zerozero.marryit.workspace.repository.WorkspaceRepository;
import com.zerozero.marryit.workspace.service.WorkspaceAccessService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({
        VendorRecommendationService.class,
        WorkspaceAccessService.class,
        VendorRecommendationServiceTest.FakeKakaoConfig.class
})
class VendorRecommendationServiceTest {

    @Autowired
    private VendorRecommendationService vendorRecommendationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Test
    void recommendsWorkspaceVendorsBeforeExternalSearch() {
        User planner = saveUser();
        Workspace workspace = saveWorkspaceWithOwner(planner);
        saveVendor(workspace, "place-1", VendorCategory.FLOWER);

        VendorRecommendationResponse response = vendorRecommendationService.recommend(
                workspace.getId(),
                planner.getId(),
                new VendorRecommendationRequest(VendorCategory.FLOWER, "강남", false)
        );

        assertThat(response.workspaceCandidates())
                .extracting(VendorCandidateResponse::source)
                .containsExactly(VendorCandidateSource.WORKSPACE);
        assertThat(response.externalCandidates()).isEmpty();
    }

    @Test
    void marksKakaoResultsAsExternalWhenNoWorkspaceVendorExists() {
        User planner = saveUser();
        Workspace workspace = saveWorkspaceWithOwner(planner);

        VendorRecommendationResponse response = vendorRecommendationService.recommend(
                workspace.getId(),
                planner.getId(),
                new VendorRecommendationRequest(VendorCategory.FLOWER, "강남", true)
        );

        assertThat(response.workspaceCandidates()).isEmpty();
        assertThat(response.externalCandidates())
                .extracting(VendorCandidateResponse::source)
                .containsExactly(VendorCandidateSource.KAKAO_EXTERNAL);
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

    private Vendor saveVendor(Workspace workspace, String kakaoPlaceId, VendorCategory category) {
        return vendorRepository.save(Vendor.create(
                workspace,
                kakaoPlaceId,
                "A 플라워",
                category,
                "서울 강남구",
                "서울 강남구 테헤란로",
                "02-123-4567",
                new BigDecimal("37.4980950"),
                new BigDecimal("127.0276100"),
                "https://place.map.kakao.com/" + kakaoPlaceId,
                true,
                "김담당",
                "내추럴 스타일"
        ));
    }

    @TestConfiguration
    static class FakeKakaoConfig {

        @Bean
        KakaoPlaceClient kakaoPlaceClient() {
            return query -> List.of(new KakaoPlaceResponse(
                    "external-1",
                    "외부 플라워",
                    "서울 강남구",
                    "서울 강남구 테헤란로",
                    "02-555-5555",
                    new BigDecimal("37.4980950"),
                    new BigDecimal("127.0276100"),
                    "https://place.map.kakao.com/external-1"
            ));
        }
    }
}
