package com.zerozero.marryit.agent.tool;

import com.zerozero.marryit.schedule.domain.ScheduleTargetType;
import com.zerozero.marryit.schedule.repository.ScheduleRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GetPlannerScheduleTool implements AgentTool {

    private final ScheduleRepository scheduleRepository;

    public GetPlannerScheduleTool(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public String name() {
        return "get_planner_schedule";
    }

    @Override
    public String description() {
        return "플래너의 일정(상담, 업체 방문, 동행 등)을 조회합니다. plannerUserId를 지정하지 않으면 "
                + "현재 요청 중인 플래너 본인의 일정을 반환합니다.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return JsonSchema.object(
                Map.of("plannerUserId", JsonSchema.property("integer", "조회할 플래너의 User ID (선택, 기본값은 현재 사용자)")),
                List.of()
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments, AgentToolContext context) {
        Long plannerUserId = AgentToolArguments.optionalLong(arguments, "plannerUserId");
        Long targetPlannerId = plannerUserId != null ? plannerUserId : context.userId();

        return scheduleRepository
                .findByWorkspaceIdAndTargetTypeAndTargetIdOrderByStartsAtAsc(
                        context.workspaceId(), ScheduleTargetType.PLANNER, targetPlannerId)
                .stream()
                .map(ScheduleResult::from)
                .toList();
    }
}
