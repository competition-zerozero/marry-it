package com.zerozero.marryit.agent.tool;

import com.zerozero.marryit.vendor.repository.VendorExperienceRepository;
import com.zerozero.marryit.vendor.repository.VendorRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GetVendorExperiencesTool implements AgentTool {

    private final VendorRepository vendorRepository;
    private final VendorExperienceRepository vendorExperienceRepository;

    public GetVendorExperiencesTool(
            VendorRepository vendorRepository,
            VendorExperienceRepository vendorExperienceRepository
    ) {
        this.vendorRepository = vendorRepository;
        this.vendorExperienceRepository = vendorExperienceRepository;
    }

    @Override
    public String name() {
        return "get_vendor_experiences";
    }

    @Override
    public String description() {
        return "플래너들이 실제로 업체를 이용하며 남긴 경험과 노하우를 조회합니다. "
                + "예: 급한 주문 대응, 체형 커버, 응답 속도 등. 업체 추천이나 대체 업체 판단 전에 반드시 확인하세요.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return JsonSchema.object(
                Map.of("vendorId", JsonSchema.property("integer", "경험을 조회할 업체 ID")),
                List.of("vendorId")
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments, AgentToolContext context) {
        Long vendorId = AgentToolArguments.requireLong(arguments, "vendorId");
        vendorRepository.findByIdAndWorkspaceId(vendorId, context.workspaceId())
                .orElseThrow(() -> new IllegalArgumentException("해당 Workspace에서 vendorId " + vendorId + "를 찾을 수 없습니다."));

        return vendorExperienceRepository.findByWorkspaceIdAndVendorIdOrderByIdDesc(context.workspaceId(), vendorId)
                .stream()
                .map(VendorExperienceResult::from)
                .toList();
    }
}
