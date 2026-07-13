package com.zerozero.marryit.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.zerozero.marryit.recommendation.service.VendorCandidateResponse;
import com.zerozero.marryit.vendor.domain.Vendor;
import com.zerozero.marryit.vendor.domain.VendorCategory;
import com.zerozero.marryit.vendor.repository.VendorRepository;
import com.zerozero.marryit.workspace.domain.Workspace;
import com.zerozero.marryit.workspace.repository.WorkspaceRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(SearchWorkspaceVendorsTool.class)
class SearchWorkspaceVendorsToolTest {

    @Autowired
    private SearchWorkspaceVendorsTool searchWorkspaceVendorsTool;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Test
    @SuppressWarnings("unchecked")
    void onlyReturnsVendorsFromTheRequestedWorkspace() {
        Workspace workspace = workspaceRepository.save(Workspace.createPersonal("워크스페이스 A"));
        Workspace otherWorkspace = workspaceRepository.save(Workspace.createPersonal("워크스페이스 B"));
        vendorRepository.save(sampleVendor(workspace, "place-1", VendorCategory.FLOWER));
        vendorRepository.save(sampleVendor(otherWorkspace, "place-2", VendorCategory.FLOWER));

        Object result = searchWorkspaceVendorsTool.execute(
                Map.of("category", "FLOWER"),
                new AgentToolContext(workspace.getId(), 1L)
        );

        List<VendorCandidateResponse> candidates = (List<VendorCandidateResponse>) result;
        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).kakaoPlaceId()).isEqualTo("place-1");
    }

    private Vendor sampleVendor(Workspace workspace, String kakaoPlaceId, VendorCategory category) {
        return Vendor.create(
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
        );
    }
}
