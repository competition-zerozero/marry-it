package com.zerozero.marryit.agent.tool;

import com.zerozero.marryit.customer.domain.Customer;
import com.zerozero.marryit.customer.repository.CustomerRepository;
import java.util.Map;

final class AgentCustomerFinder {

    private AgentCustomerFinder() {
    }

    static Customer find(CustomerRepository customerRepository, Map<String, Object> arguments, AgentToolContext context) {
        Long customerId = AgentToolArguments.optionalLong(arguments, "customerId");
        if (customerId != null) {
            return customerRepository.findByIdAndWorkspaceId(customerId, context.workspaceId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "해당 Workspace에서 customerId " + customerId + "를 찾을 수 없습니다."));
        }

        String customerName = AgentToolArguments.optionalString(arguments, "customerName");
        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("customerId 또는 customerName이 필요합니다.");
        }

        String keyword = customerName.trim().toLowerCase();
        return customerRepository.findByWorkspaceIdOrderByIdDesc(context.workspaceId()).stream()
                .filter(customer -> contains(customer.getBrideName(), keyword) || contains(customer.getGroomName(), keyword))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 Workspace에서 customerName " + customerName + "을 찾을 수 없습니다."));
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }
}
