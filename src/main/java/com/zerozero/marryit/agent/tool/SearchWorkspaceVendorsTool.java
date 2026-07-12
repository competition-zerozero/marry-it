package com.zerozero.marryit.agent.tool;

import com.zerozero.marryit.recommendation.service.VendorCandidateResponse;
import com.zerozero.marryit.vendor.domain.Vendor;
import com.zerozero.marryit.vendor.domain.VendorCategory;
import com.zerozero.marryit.vendor.repository.VendorRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SearchWorkspaceVendorsTool implements AgentTool {

    private static final String REASON = "Workspace에 등록된 기존 거래처입니다. 실제 예약 가능 여부와 계약 조건은 일정/경험 데이터로 추가 확인하세요.";

    private final VendorRepository vendorRepository;

    public SearchWorkspaceVendorsTool(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    @Override
    public String name() {
        return "search_workspace_vendors";
    }

    @Override
    public String description() {
        return "현재 Workspace에 이미 등록된 기존 거래 업체를 카테고리와 키워드로 검색합니다. "
                + "새 업체를 카카오맵에서 찾기 전에 항상 먼저 호출해서 기존 거래처로 해결되는지 확인하세요.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return JsonSchema.object(
                Map.of(
                        "category", JsonSchema.enumProperty(
                                "string",
                                "업체 카테고리",
                                Arrays.stream(VendorCategory.values()).map(Enum::name).toList()
                        ),
                        "keyword", JsonSchema.property("string", "업체명 또는 주소에 포함된 검색어 (선택)")
                ),
                List.of()
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments, AgentToolContext context) {
        String categoryValue = AgentToolArguments.optionalString(arguments, "category");
        String keyword = AgentToolArguments.optionalString(arguments, "keyword");

        List<Vendor> vendors = categoryValue == null
                ? vendorRepository.findByWorkspaceIdOrderByIdDesc(context.workspaceId())
                : vendorRepository.findByWorkspaceIdAndCategoryOrderByIdDesc(
                        context.workspaceId(), VendorCategory.valueOf(categoryValue));

        return vendors.stream()
                .filter(vendor -> matches(vendor, keyword))
                .map(vendor -> VendorCandidateResponse.workspaceVendor(vendor, REASON))
                .toList();
    }

    private boolean matches(Vendor vendor, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.trim();
        return contains(vendor.getName(), normalized)
                || contains(vendor.getAddress(), normalized)
                || contains(vendor.getRoadAddress(), normalized);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }
}
