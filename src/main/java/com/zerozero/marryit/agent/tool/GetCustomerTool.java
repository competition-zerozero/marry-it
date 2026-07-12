package com.zerozero.marryit.agent.tool;

import com.zerozero.marryit.customer.domain.Customer;
import com.zerozero.marryit.customer.repository.CustomerRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GetCustomerTool implements AgentTool {

    private final CustomerRepository customerRepository;

    public GetCustomerTool(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public String name() {
        return "get_customer";
    }

    @Override
    public String description() {
        return "고객 하나의 상세 정보를 조회합니다. 기본 정보, 결혼 정보, 취향, 예산, D-Day, 상담/할 일 메모를 포함합니다. "
                + "확인되지 않은 취향이나 예산을 추측하지 말고 이 결과만 사용하세요.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return JsonSchema.object(
                Map.of("customerId", JsonSchema.property("integer", "조회할 고객의 ID")),
                List.of("customerId")
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments, AgentToolContext context) {
        Long customerId = AgentToolArguments.requireLong(arguments, "customerId");
        Customer customer = customerRepository.findByIdAndWorkspaceId(customerId, context.workspaceId())
                .orElseThrow(() -> new IllegalArgumentException("해당 Workspace에서 customerId " + customerId + "를 찾을 수 없습니다."));
        return CustomerDetailResult.from(customer);
    }
}
