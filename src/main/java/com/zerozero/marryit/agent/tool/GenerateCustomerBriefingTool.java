package com.zerozero.marryit.agent.tool;

import com.zerozero.marryit.customer.domain.Customer;
import com.zerozero.marryit.customer.repository.CustomerRepository;
import com.zerozero.marryit.schedule.domain.ScheduleTargetType;
import com.zerozero.marryit.schedule.repository.ScheduleRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GenerateCustomerBriefingTool implements AgentTool {

    private final CustomerRepository customerRepository;
    private final ScheduleRepository scheduleRepository;

    public GenerateCustomerBriefingTool(CustomerRepository customerRepository, ScheduleRepository scheduleRepository) {
        this.customerRepository = customerRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public String name() {
        return "generate_customer_briefing";
    }

    @Override
    public String description() {
        return "고객 상담 전에 확인해야 할 고객 성향, 선호 스타일, 예산 현황, 진행 상황, 미완료 업무, 최근 문제, 결정 사항을 요약합니다. "
                + "확인되지 않은 계약/예산/문제는 추측하지 않습니다.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return JsonSchema.object(
                Map.of(
                        "customerId", JsonSchema.property("integer", "브리핑할 고객 ID"),
                        "customerName", JsonSchema.property("string", "브리핑할 고객 이름 일부. customerId가 없을 때 사용")
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

        CustomerWeddingContextResult weddingContext = CustomerWeddingContextResult.of(
                customer,
                List.of(),
                schedules,
                List.of("계약 업체와 사용 예산은 아직 구조화 데이터가 없어 브리핑에서 확정 정보로 제공하지 않습니다.")
        );

        return new CustomerBriefingResult(
                weddingContext,
                customer.getImportantConditions(),
                customer.getPreferredStyle(),
                budgetStatus(customer),
                progressSummary(customer),
                List.of(),
                customer.getTodoMemo(),
                recentProblems(customer),
                decisionsForConsultation(customer),
                cautions(customer)
        );
    }

    private String budgetStatus(Customer customer) {
        if (customer.getTotalBudget() == null) {
            return "총예산이 등록되어 있지 않습니다.";
        }
        return "총예산 " + customer.getTotalBudget() + "원. 사용 예산과 남은 예산은 구조화 지출 데이터가 없어 계산할 수 없습니다.";
    }

    private String progressSummary(Customer customer) {
        StringBuilder summary = new StringBuilder();
        if (customer.getCompletedMemo() != null && !customer.getCompletedMemo().isBlank()) {
            summary.append("완료: ").append(customer.getCompletedMemo());
        }
        if (customer.getTodoMemo() != null && !customer.getTodoMemo().isBlank()) {
            if (!summary.isEmpty()) summary.append(" / ");
            summary.append("미완료: ").append(customer.getTodoMemo());
        }
        return summary.isEmpty() ? "진행 메모가 등록되어 있지 않습니다." : summary.toString();
    }

    private String recentProblems(Customer customer) {
        List<String> problems = new ArrayList<>();
        addIfProblem(problems, customer.getConsultationMemo());
        addIfProblem(problems, customer.getTodoMemo());
        return problems.isEmpty() ? "최근 문제로 확인된 구조화 데이터가 없습니다." : String.join(" / ", problems);
    }

    private List<String> decisionsForConsultation(Customer customer) {
        List<String> decisions = new ArrayList<>();
        if (customer.getTodoMemo() != null && !customer.getTodoMemo().isBlank()) {
            decisions.add("미완료 업무 처리 방향 확인: " + customer.getTodoMemo());
        }
        if (customer.getItemBudgetMemo() != null && !customer.getItemBudgetMemo().isBlank()) {
            decisions.add("항목별 예산 확정 또는 조정: " + customer.getItemBudgetMemo());
        }
        if (decisions.isEmpty()) {
            decisions.add("상담에서 결정할 사항이 구조화되어 있지 않습니다.");
        }
        return decisions;
    }

    private List<String> cautions(Customer customer) {
        List<String> cautions = new ArrayList<>();
        if (customer.getAvoidConditions() != null && !customer.getAvoidConditions().isBlank()) {
            cautions.add("피해야 할 조건: " + customer.getAvoidConditions());
        }
        if (customer.getConsultationMemo() != null && !customer.getConsultationMemo().isBlank()) {
            cautions.add("상담 메모 확인: " + customer.getConsultationMemo());
        }
        return cautions;
    }

    private void addIfProblem(List<String> problems, String memo) {
        if (memo == null) return;
        if (memo.contains("취소") || memo.contains("문제") || memo.contains("지연") || memo.contains("확인")) {
            problems.add(memo);
        }
    }
}
