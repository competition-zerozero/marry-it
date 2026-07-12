package com.zerozero.marryit.agent.tool;

import com.zerozero.marryit.schedule.domain.ScheduleTargetType;
import com.zerozero.marryit.schedule.repository.ScheduleRepository;
import com.zerozero.marryit.vendor.repository.VendorRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GetVendorScheduleTool implements AgentTool {

    private final VendorRepository vendorRepository;
    private final ScheduleRepository scheduleRepository;

    public GetVendorScheduleTool(VendorRepository vendorRepository, ScheduleRepository scheduleRepository) {
        this.vendorRepository = vendorRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public String name() {
        return "get_vendor_schedule";
    }

    @Override
    public String description() {
        return "업체의 예약/방문/계약 일정을 조회합니다. 특정 기간에 업체가 가능한지 판단할 때 사용하세요.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return JsonSchema.object(
                Map.of("vendorId", JsonSchema.property("integer", "일정을 조회할 업체 ID")),
                List.of("vendorId")
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments, AgentToolContext context) {
        Long vendorId = AgentToolArguments.requireLong(arguments, "vendorId");
        vendorRepository.findByIdAndWorkspaceId(vendorId, context.workspaceId())
                .orElseThrow(() -> new IllegalArgumentException("해당 Workspace에서 vendorId " + vendorId + "를 찾을 수 없습니다."));

        return scheduleRepository
                .findByWorkspaceIdAndTargetTypeAndTargetIdOrderByStartsAtAsc(
                        context.workspaceId(), ScheduleTargetType.VENDOR, vendorId)
                .stream()
                .map(ScheduleResult::from)
                .toList();
    }
}
