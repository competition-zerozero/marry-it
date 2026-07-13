package com.zerozero.marryit.agent.tool;

import com.zerozero.marryit.customer.domain.Customer;
import com.zerozero.marryit.customer.repository.CustomerRepository;
import com.zerozero.marryit.schedule.domain.ScheduleTargetType;
import com.zerozero.marryit.schedule.repository.ScheduleRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GetCustomerWeddingContextTool implements AgentTool {

    private final CustomerRepository customerRepository;
    private final ScheduleRepository scheduleRepository;

    public GetCustomerWeddingContextTool(CustomerRepository customerRepository, ScheduleRepository scheduleRepository) {
        this.customerRepository = customerRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public String name() {
        return "get_customer_wedding_context";
    }

    @Override
    public String description() {
        return "특정 고객의 결혼 준비 현황을 한 번에 조회합니다. 고객 기본 정보, 예식일/지역, 예산, 취향, 일정, 미완료 업무를 반환합니다. "
                + "계약 업체와 사용 예산은 아직 구조화 데이터가 없으면 추측하지 않고 unknown으로 둡니다.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return JsonSchema.object(
                Map.of(
                        "customerId", JsonSchema.property("integer", "조회할 고객 ID. 알 수 있으면 이 값을 우선 사용"),
                        "customerName", JsonSchema.property("string", "조회할 고객 이름 일부. customerId가 없을 때 사용")
                ),
                List.of()
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments, AgentToolContext context) {
        Customer customer = AgentCustomerFinder.find(customerRepository, arguments, context);
        List<ScheduleResult> schedules = scheduleRepository
                .findByWorkspaceIdAndTargetTypeAndTargetIdOrderByStartsAtAsc(
                        context.workspaceId(), ScheduleTargetType.CUSTOMER, customer.getId())
                .stream()
                .map(ScheduleResult::from)
                .toList();

        return CustomerWeddingContextResult.of(
                customer,
                List.of(),
                schedules,
                List.of(
                        "계약 업체를 연결하는 계약 도메인이 아직 없어 contractedVendors는 비어 있습니다.",
                        "사용 예산과 남은 예산을 계산할 구조화 계약/지출 데이터가 아직 없어 usedBudget, remainingBudget은 null입니다."
                )
        );
    }
}
