package com.zerozero.marryit.agent.tool;

import com.zerozero.marryit.vendor.domain.Vendor;
import com.zerozero.marryit.vendor.repository.VendorRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GetVendorTool implements AgentTool {

    private final VendorRepository vendorRepository;

    public GetVendorTool(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    @Override
    public String name() {
        return "get_vendor";
    }

    @Override
    public String description() {
        return "Workspace에 등록된 업체 하나의 상세 정보(연락처, 주소, 제휴 여부 등)를 조회합니다.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return JsonSchema.object(
                Map.of("vendorId", JsonSchema.property("integer", "조회할 업체 ID")),
                List.of("vendorId")
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments, AgentToolContext context) {
        Long vendorId = AgentToolArguments.requireLong(arguments, "vendorId");
        Vendor vendor = vendorRepository.findByIdAndWorkspaceId(vendorId, context.workspaceId())
                .orElseThrow(() -> new IllegalArgumentException("해당 Workspace에서 vendorId " + vendorId + "를 찾을 수 없습니다."));
        return VendorDetailResult.from(vendor);
    }
}
