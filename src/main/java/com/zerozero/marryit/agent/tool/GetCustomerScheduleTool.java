package com.zerozero.marryit.agent.tool;

import com.zerozero.marryit.customer.domain.Customer;
import com.zerozero.marryit.customer.repository.CustomerRepository;
import com.zerozero.marryit.schedule.domain.ScheduleTargetType;
import com.zerozero.marryit.schedule.repository.ScheduleRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GetCustomerScheduleTool implements AgentTool {

    private final CustomerRepository customerRepository;
    private final ScheduleRepository scheduleRepository;

    public GetCustomerScheduleTool(CustomerRepository customerRepository, ScheduleRepository scheduleRepository) {
        this.customerRepository = customerRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public String name() {
        return "get_customer_schedule";
    }

    @Override
    public String description() {
        return "고객의 상담, 웨딩홀 투어, 드레스 피팅, 본식 등 일정 목록을 조회합니다.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return JsonSchema.object(
                Map.of("customerId", JsonSchema.property("integer", "일정을 조회할 고객 ID")),
                List.of("customerId")
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments, AgentToolContext context) {
        Long customerId = AgentToolArguments.requireLong(arguments, "customerId");
        Customer customer = customerRepository.findByIdAndWorkspaceId(customerId, context.workspaceId())
                .orElseThrow(() -> new IllegalArgumentException("해당 Workspace에서 customerId " + customerId + "를 찾을 수 없습니다."));

        return scheduleRepository
                .findByWorkspaceIdAndTargetTypeAndTargetIdOrderByStartsAtAsc(
                        context.workspaceId(), ScheduleTargetType.CUSTOMER, customer.getId())
                .stream()
                .map(ScheduleResult::from)
                .toList();
    }
}
