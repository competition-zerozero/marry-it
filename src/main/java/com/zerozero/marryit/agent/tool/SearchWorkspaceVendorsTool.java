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
                        "nameKeyword", JsonSchema.property("string", "업체명에 포함된 검색어 (선택)"),
                        "areaKeyword", JsonSchema.property("string", "주소(지역)에 포함된 검색어. 지역 조건이 있을 때만 사용 (선택)")
                ),
                List.of()
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments, AgentToolContext context) {
        String categoryValue = AgentToolArguments.optionalString(arguments, "category");
        String nameKeyword = AgentToolArguments.optionalString(arguments, "nameKeyword");
        String areaKeyword = AgentToolArguments.optionalString(arguments, "areaKeyword");

        List<Vendor> vendors = categoryValue == null
                ? vendorRepository.findByWorkspaceIdOrderByIdDesc(context.workspaceId())
                : vendorRepository.findByWorkspaceIdAndCategoryOrderByIdDesc(
                        context.workspaceId(), VendorCategory.valueOf(categoryValue));

        return vendors.stream()
                .filter(vendor -> matchesName(vendor, nameKeyword))
                .filter(vendor -> matchesArea(vendor, areaKeyword))
                .map(vendor -> VendorCandidateResponse.workspaceVendor(vendor, buildReason(vendor)))
                .toList();
    }

    private boolean matchesName(Vendor vendor, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        return contains(vendor.getName(), keyword.trim());
    }

    private boolean matchesArea(Vendor vendor, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String k = keyword.trim();
        return contains(vendor.getAddress(), k) || contains(vendor.getRoadAddress(), k);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword.toLowerCase());
    }

    private String buildReason(Vendor vendor) {
        StringBuilder sb = new StringBuilder();
        if (vendor.getMemo() != null && !vendor.getMemo().isBlank()) {
            sb.append(vendor.getMemo());
        }
        if (vendor.getContactPerson() != null && !vendor.getContactPerson().isBlank()) {
            if (!sb.isEmpty()) sb.append(" · ");
            sb.append("담당: ").append(vendor.getContactPerson());
        }
        if (vendor.isPartnered()) {
            if (!sb.isEmpty()) sb.append(" · ");
            sb.append("제휴 업체");
        }
        if (sb.isEmpty()) {
            sb.append("등록된 거래처입니다.");
        }
        return sb.toString();
    }
}
