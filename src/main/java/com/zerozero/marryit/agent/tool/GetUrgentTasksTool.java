package com.zerozero.marryit.agent.tool;

import com.zerozero.marryit.customer.domain.Customer;
import com.zerozero.marryit.customer.repository.CustomerRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GetUrgentTasksTool implements AgentTool {

    private final CustomerRepository customerRepository;

    public GetUrgentTasksTool(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public String name() {
        return "get_urgent_tasks";
    }

    @Override
    public String description() {
        return "웨딩 플래너가 현재 가장 먼저 처리해야 하는 업무를 긴급도 순서대로 조회합니다. "
                + "현재 MVP에서는 고객의 예식일까지 남은 기간과 todo/상담 메모의 취소·확인·지연 키워드를 기준으로 판단합니다.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return JsonSchema.object(
                Map.of("limit", JsonSchema.property("integer", "최대 반환 개수")),
                List.of()
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments, AgentToolContext context) {
        int limit = Math.toIntExact(AgentToolArguments.optionalLong(arguments, "limit") == null
                ? 10L
                : AgentToolArguments.optionalLong(arguments, "limit"));

        return customerRepository.findByWorkspaceIdOrderByIdDesc(context.workspaceId()).stream()
                .flatMap(customer -> tasksFor(customer).stream())
                .sorted(Comparator.comparingInt(this::urgencyOrder)
                        .thenComparing(task -> task.dueDate() == null ? LocalDate.MAX : task.dueDate()))
                .limit(limit)
                .toList();
    }

    private List<UrgentTaskResult> tasksFor(Customer customer) {
        List<UrgentTaskResult> tasks = new ArrayList<>();
        Integer dDay = WeddingDDay.daysUntil(customer.getWeddingDate());
        String customerName = customer.getBrideName() + "·" + customer.getGroomName();

        if (customer.getTodoMemo() != null && !customer.getTodoMemo().isBlank()) {
            tasks.add(new UrgentTaskResult(
                    customer.getId(),
                    customerName,
                    customer.getTodoMemo(),
                    urgency(customer.getTodoMemo(), dDay),
                    urgentReason(customer.getTodoMemo(), dDay),
                    customer.getWeddingDate(),
                    dDay
            ));
        }
        if (dDay != null && dDay >= 0 && dDay <= 7) {
            tasks.add(new UrgentTaskResult(
                    customer.getId(),
                    customerName,
                    "예식 임박 고객 최종 점검",
                    dDay <= 3 ? "HIGH" : "MEDIUM",
                    "예식일까지 " + dDay + "일 남았습니다.",
                    customer.getWeddingDate(),
                    dDay
            ));
        }
        if (containsProblem(customer.getConsultationMemo())) {
            tasks.add(new UrgentTaskResult(
                    customer.getId(),
                    customerName,
                    customer.getConsultationMemo(),
                    "HIGH",
                    "상담 메모에 취소/문제/지연/확인 대기 키워드가 있습니다.",
                    customer.getWeddingDate(),
                    dDay
            ));
        }
        return tasks;
    }

    private String urgency(String memo, Integer dDay) {
        if (containsProblem(memo) || (dDay != null && dDay <= 3)) return "HIGH";
        if (dDay != null && dDay <= 14) return "MEDIUM";
        return "LOW";
    }

    private String urgentReason(String memo, Integer dDay) {
        if (containsProblem(memo)) return "업무 메모에 취소/문제/지연/확인 대기 키워드가 있습니다.";
        if (dDay != null) return "예식일까지 " + dDay + "일 남았습니다.";
        return "업무 메모가 미완료 상태입니다.";
    }

    private boolean containsProblem(String value) {
        if (value == null) return false;
        return value.contains("취소") || value.contains("문제") || value.contains("지연") || value.contains("확인");
    }

    private int urgencyOrder(UrgentTaskResult task) {
        return switch (task.urgency()) {
            case "HIGH" -> 0;
            case "MEDIUM" -> 1;
            default -> 2;
        };
    }
}
