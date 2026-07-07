package com.zerozero.marryit.vendor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zerozero.marryit.auth.domain.OAuthProvider;
import com.zerozero.marryit.auth.domain.User;
import com.zerozero.marryit.auth.repository.UserRepository;
import com.zerozero.marryit.vendor.domain.Vendor;
import com.zerozero.marryit.vendor.domain.VendorCategory;
import com.zerozero.marryit.vendor.repository.VendorExperienceRepository;
import com.zerozero.marryit.vendor.repository.VendorRepository;
import com.zerozero.marryit.workspace.domain.Workspace;
import com.zerozero.marryit.workspace.domain.WorkspaceMember;
import com.zerozero.marryit.workspace.repository.WorkspaceMemberRepository;
import com.zerozero.marryit.workspace.repository.WorkspaceRepository;
import com.zerozero.marryit.workspace.service.WorkspaceAccessService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({VendorExperienceService.class, WorkspaceAccessService.class})
class VendorExperienceServiceTest {

    @Autowired
    private VendorExperienceService vendorExperienceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private VendorExperienceRepository vendorExperienceRepository;

    @Test
    void createsAndListsVendorExperienceInsideWorkspace() {
        User planner = saveUser("google-1", "planner@example.com", "서영");
        Workspace workspace = saveWorkspaceWithOwner(planner);
        Vendor vendor = saveVendor(workspace, "place-1");

        VendorExperienceResponse created = vendorExperienceService.create(
                workspace.getId(),
                vendor.getId(),
                planner.getId(),
                new VendorExperienceRequest("급한 주문 대응이 빠르고 화이트톤 부케를 잘함")
        );

        assertThat(created.id()).isNotNull();
        assertThat(created.plannerName()).isEqualTo("서영");
        assertThat(vendorExperienceService.findByVendor(workspace.getId(), vendor.getId(), planner.getId()))
                .extracting(VendorExperienceResponse::content)
                .containsExactly("급한 주문 대응이 빠르고 화이트톤 부케를 잘함");
    }

    @Test
    void blocksExperienceForVendorInDifferentWorkspace() {
        User planner = saveUser("google-1", "planner@example.com", "서영");
        Workspace workspace = saveWorkspaceWithOwner(planner);
        Workspace otherWorkspace = workspaceRepository.save(Workspace.createPersonal("다른"));
        workspaceMemberRepository.save(WorkspaceMember.owner(planner, otherWorkspace));
        Vendor vendor = saveVendor(workspace, "place-1");

        assertThatThrownBy(() -> vendorExperienceService.create(
                otherWorkspace.getId(),
                vendor.getId(),
                planner.getId(),
                new VendorExperienceRequest("다른 워크스페이스 접근")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vendor not found");

        assertThat(vendorExperienceRepository.count()).isZero();
    }

    private User saveUser(String providerUserId, String email, String name) {
        return userRepository.save(User.createOAuthUser(OAuthProvider.GOOGLE, providerUserId, email, name, null));
    }

    private Workspace saveWorkspaceWithOwner(User owner) {
        Workspace workspace = workspaceRepository.save(Workspace.createPersonal(owner.getName()));
        workspaceMemberRepository.save(WorkspaceMember.owner(owner, workspace));
        return workspace;
    }

    private Vendor saveVendor(Workspace workspace, String kakaoPlaceId) {
        return vendorRepository.save(Vendor.create(
                workspace,
                kakaoPlaceId,
                "A 플라워",
                VendorCategory.FLOWER,
                "서울 강남구",
                "서울 강남구 테헤란로",
                "02-123-4567",
                new BigDecimal("37.4980950"),
                new BigDecimal("127.0276100"),
                "https://place.map.kakao.com/" + kakaoPlaceId,
                true,
                "김담당"
        ));
    }
}
