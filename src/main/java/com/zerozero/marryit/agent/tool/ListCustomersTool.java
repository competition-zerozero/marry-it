package com.zerozero.marryit.agent.tool;

import com.zerozero.marryit.customer.domain.Customer;
import com.zerozero.marryit.customer.repository.CustomerRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ListCustomersTool implements AgentTool {

    private final CustomerRepository customerRepository;

    public ListCustomersTool(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public String name() {
        return "list_customers";
    }

    @Override
    public String description() {
        return "현재 Workspace에 등록된 고객(커플)을 신랑/신부 이름 키워드로 검색합니다. "
                + "고객 이름이 언급됐지만 customerId를 모를 때 가장 먼저 호출하세요.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return JsonSchema.object(
                Map.of("keyword", JsonSchema.property("string", "신랑 또는 신부 이름에 포함된 검색어. 비워두면 최근 등록 고객을 반환합니다.")),
                List.of()
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments, AgentToolContext context) {
        String keyword = (String) arguments.get("keyword");
        List<Customer> customers = customerRepository.findByWorkspaceIdOrderByIdDesc(context.workspaceId());

        return customers.stream()
                .filter(customer -> matches(customer, keyword))
                .limit(20)
                .map(CustomerSummaryResult::from)
                .toList();
    }

    private boolean matches(Customer customer, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.trim();
        return contains(customer.getGroomName(), normalized) || contains(customer.getBrideName(), normalized);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }
}
