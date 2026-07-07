package com.zerozero.marryit.vendor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zerozero.marryit.auth.domain.OAuthProvider;
import com.zerozero.marryit.auth.domain.User;
import com.zerozero.marryit.auth.repository.UserRepository;
import com.zerozero.marryit.vendor.domain.VendorCategory;
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
@Import({VendorService.class, WorkspaceAccessService.class})
class VendorServiceTest {

    @Autowired
    private VendorService vendorService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Test
    void createsVendorFromKakaoPlaceInsideWorkspace() {
        User planner = saveUser("google-1", "planner@example.com", "서영");
        Workspace workspace = saveWorkspaceWithOwner(planner);

        VendorResponse created = vendorService.create(workspace.getId(), planner.getId(), sampleRequest("place-1"));

        assertThat(created.id()).isNotNull();
        assertThat(created.workspaceId()).isEqualTo(workspace.getId());
        assertThat(created.kakaoPlaceId()).isEqualTo("place-1");
        assertThat(created.category()).isEqualTo(VendorCategory.FLOWER);
    }

    @Test
    void blocksDuplicateKakaoPlaceInsideSameWorkspace() {
        User planner = saveUser("google-1", "planner@example.com", "서영");
        Workspace workspace = saveWorkspaceWithOwner(planner);
        vendorService.create(workspace.getId(), planner.getId(), sampleRequest("place-1"));

        assertThatThrownBy(() -> vendorService.create(workspace.getId(), planner.getId(), sampleRequest("place-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void allowsSameKakaoPlaceAcrossDifferentWorkspaces() {
        User planner = saveUser("google-1", "planner@example.com", "서영");
        Workspace workspace = saveWorkspaceWithOwner(planner);
        Workspace otherWorkspace = workspaceRepository.save(Workspace.createPersonal("다른"));
        workspaceMemberRepository.save(WorkspaceMember.owner(planner, otherWorkspace));

        vendorService.create(workspace.getId(), planner.getId(), sampleRequest("place-1"));
        vendorService.create(otherWorkspace.getId(), planner.getId(), sampleRequest("place-1"));

        assertThat(vendorRepository.count()).isEqualTo(2);
    }

    @Test
    void blocksNonMemberFromCreatingVendor() {
        User owner = saveUser("google-1", "owner@example.com", "오너");
        User outsider = saveUser("google-2", "outsider@example.com", "외부인");
        Workspace workspace = saveWorkspaceWithOwner(owner);

        assertThatThrownBy(() -> vendorService.create(workspace.getId(), outsider.getId(), sampleRequest("place-1")))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void updatesAndDeletesVendorInsideWorkspace() {
        User planner = saveUser("google-1", "planner@example.com", "서영");
        Workspace workspace = saveWorkspaceWithOwner(planner);
        VendorResponse created = vendorService.create(workspace.getId(), planner.getId(), sampleRequest("place-1"));

        VendorResponse updated = vendorService.update(
                workspace.getId(),
                planner.getId(),
                created.id(),
                new VendorRequest(
                        "place-1",
                        "B 플라워",
                        VendorCategory.FLOWER,
                        "서울 서초구",
                        "서울 서초구 반포대로",
                        "02-987-6543",
                        new BigDecimal("37.5000000"),
                        new BigDecimal("127.0000000"),
                        "https://place.map.kakao.com/place-1",
                        false,
                        "박담당"
                )
        );
        vendorService.delete(workspace.getId(), planner.getId(), created.id());

        assertThat(updated.name()).isEqualTo("B 플라워");
        assertThat(updated.partnered()).isFalse();
        assertThat(vendorRepository.count()).isZero();
    }

    private User saveUser(String providerUserId, String email, String name) {
        return userRepository.save(User.createOAuthUser(
                OAuthProvider.GOOGLE,
                providerUserId,
                email,
                name,
                null
        ));
    }

    private Workspace saveWorkspaceWithOwner(User owner) {
        Workspace workspace = workspaceRepository.save(Workspace.createPersonal(owner.getName()));
        workspaceMemberRepository.save(WorkspaceMember.owner(owner, workspace));
        return workspace;
    }

    private VendorRequest sampleRequest(String kakaoPlaceId) {
        return new VendorRequest(
                kakaoPlaceId,
                "A 플라워",
                VendorCategory.FLOWER,
                "서울 강남구 테헤란로 1",
                "서울 강남구 테헤란로 1",
                "02-123-4567",
                new BigDecimal("37.4980950"),
                new BigDecimal("127.0276100"),
                "https://place.map.kakao.com/" + kakaoPlaceId,
                true,
                "김담당"
        );
    }
}
